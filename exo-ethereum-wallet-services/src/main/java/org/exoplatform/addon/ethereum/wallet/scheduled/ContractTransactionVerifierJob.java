package org.exoplatform.addon.ethereum.wallet.scheduled;

import static org.exoplatform.addon.ethereum.wallet.utils.WalletUtils.*;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.*;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class ContractTransactionVerifierJob implements Job {

  private static final Log           LOG = ExoLogger.getLogger(ContractTransactionVerifierJob.class);

  private ExoContainer               container;

  private EthereumClientConnector    ethereumClientConnector;

  private EthereumTransactionDecoder ethereumTransactionDecoder;

  private WalletService              walletService;

  private WalletContractService      contractService;

  private WalletTransactionService   transactionService;

  private SettingService             settingService;

  public ContractTransactionVerifierJob() {
    this(PortalContainer.getInstance());
  }

  public ContractTransactionVerifierJob(ExoContainer container) {
    this.container = container;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      GlobalSettings settings = getWalletService().getSettings();
      if (settings == null || settings.getDefaultNetworkId() == null) {
        LOG.debug("Empty network id on settings, ignore blockchain listening");
        return;
      }
      String wsURL = settings.getWebsocketProviderURL();
      if (StringUtils.isBlank(wsURL)) {
        LOG.debug("Empty Websocket URL, skip contract transaction verification");
        return;
      }
      Long networkId = settings.getDefaultNetworkId();
      Set<String> defaultContractsAddresses = getContractService().getDefaultContractsAddresses(networkId);
      if (defaultContractsAddresses == null || defaultContractsAddresses.isEmpty()) {
        LOG.debug("Empty contracts list in settings");
        return;
      }
      long lastEthereumBlockNumber = getEthereumClientConnector().getLastestBlockNumber();
      long lastWatchedBlockNumber = getLastWatchedBlockNumber(networkId);
      if (lastEthereumBlockNumber <= lastWatchedBlockNumber) {
        LOG.debug("No new blocks to verify. last watched = {}. last blockchain block = {}",
                  lastWatchedBlockNumber,
                  lastEthereumBlockNumber);
        return;
      }

      boolean processed = true;
      for (String contractAddress : defaultContractsAddresses) {
        Set<String> transactionHashes = getEthereumClientConnector().getContractTransactions(contractAddress,
                                                                                             lastWatchedBlockNumber,
                                                                                             lastEthereumBlockNumber);

        LOG.debug("{} transactions has been found on contract {} between block {} and {}",
                  transactionHashes.size(),
                  contractAddress,
                  lastWatchedBlockNumber,
                  lastEthereumBlockNumber);

        ContractDetail contractDetail = getContractService().getContractDetail(contractAddress, networkId);
        int addedTransactionsCount = 0;
        int modifiedTransactionsCount = 0;
        for (String transactionHash : transactionHashes) {
          TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(transactionHash);
          if (transactionDetail == null) {
            processed = processTransaction(networkId, transactionHash, contractDetail) && processed;
            if (processed) {
              addedTransactionsCount++;
            }
          } else {
            LOG.debug(" - transaction {} already exists on database, ignore it.", transactionDetail);
            boolean changed = false;

            // Verify that the transaction has been decoded
            if (StringUtils.isBlank(transactionDetail.getContractAddress())
                || StringUtils.isBlank(transactionDetail.getContractMethodName())) {
              transactionDetail.setContractAddress(contractAddress);
              getEthereumTransactionDecoder().computeTransactionDetail(transactionDetail, contractDetail);
              changed = true;
            }

            // Broadcast event and send notifications when the transaction was
            // marked as pending
            boolean braodcastSavingTransaction = transactionDetail.isPending();
            transactionDetail.setPending(false);
            changed = changed || braodcastSavingTransaction;

            // If the transaction wasn't marked as succeeded, try to verify the
            // status from Blockchain again
            if (!transactionDetail.isSucceeded()) {
              TransactionReceipt transactionReceipt = ethereumClientConnector.getTransactionReceipt(transactionHash);
              transactionDetail.setSucceeded(transactionReceipt != null && transactionReceipt.isStatusOK());
              changed = true;
            }

            // Save decoded transaction details after chaging its attributes
            if (changed) {
              getTransactionService().saveTransactionDetail(transactionDetail, braodcastSavingTransaction);
              modifiedTransactionsCount++;
            }
          }
        }

        LOG.debug("{} added and {} modified transactions has been stored using contract {} between block {} and {}",
                  addedTransactionsCount,
                  modifiedTransactionsCount,
                  contractAddress,
                  lastWatchedBlockNumber,
                  lastEthereumBlockNumber);
      }

      if (processed) {
        // Save last verified block for contracts transactions
        saveLastWatchedBlockNumber(networkId, lastEthereumBlockNumber);
      }
    } catch (Exception e) {
      LOG.error("Error while checking pending transactions", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  private boolean processTransaction(Long networkId, String transactionHash, ContractDetail contractDetail) {
    boolean processed = true;
    try {
      LOG.debug(" - treating transaction {} that doesn't exist on database.", transactionHash);

      TransactionDetail transactionDetail = getEthereumTransactionDecoder().computeTransactionDetail(networkId,
                                                                                                     transactionHash,
                                                                                                     contractDetail);
      if (transactionDetail == null) {
        throw new IllegalStateException("Empty transaction detail is returned");
      }
      LOG.info("Saving new transaction that wasn't managed by UI: {}", transactionDetail);
      getTransactionService().saveTransactionDetail(transactionDetail, true);
    } catch (Exception e) {
      processed = false;
      LOG.warn("Error processing transaction {}. It will be retried next time.", transactionHash, e);
    }
    return processed;
  }

  private long getLastWatchedBlockNumber(long networkId) {
    SettingValue<?> lastBlockNumberValue =
                                         getSettingService().get(WALLET_CONTEXT,
                                                                 WALLET_SCOPE,
                                                                 LAST_BLOCK_NUMBER_KEY_NAME + networkId);
    if (lastBlockNumberValue != null && lastBlockNumberValue.getValue() != null) {
      return Long.valueOf(lastBlockNumberValue.getValue().toString());
    }
    return 0;
  }

  private void saveLastWatchedBlockNumber(long networkId, long lastWatchedBlockNumber) {
    LOG.debug("Save watched block number {} on network {}", lastWatchedBlockNumber, networkId);
    getSettingService().set(WALLET_CONTEXT,
                            WALLET_SCOPE,
                            LAST_BLOCK_NUMBER_KEY_NAME + networkId,
                            SettingValue.create(lastWatchedBlockNumber));
  }

  private WalletService getWalletService() {
    if (walletService == null) {
      walletService = CommonsUtils.getService(WalletService.class);
    }
    return walletService;
  }

  private WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
  }

  private WalletContractService getContractService() {
    if (contractService == null) {
      contractService = CommonsUtils.getService(WalletContractService.class);
    }
    return contractService;
  }

  private EthereumTransactionDecoder getEthereumTransactionDecoder() {
    if (ethereumTransactionDecoder == null) {
      ethereumTransactionDecoder = CommonsUtils.getService(EthereumTransactionDecoder.class);
    }
    return ethereumTransactionDecoder;
  }

  private EthereumClientConnector getEthereumClientConnector() {
    if (ethereumClientConnector == null) {
      ethereumClientConnector = CommonsUtils.getService(EthereumClientConnector.class);
    }
    return ethereumClientConnector;
  }

  private SettingService getSettingService() {
    if (settingService == null) {
      settingService = CommonsUtils.getService(SettingService.class);
    }
    return settingService;
  }
}
