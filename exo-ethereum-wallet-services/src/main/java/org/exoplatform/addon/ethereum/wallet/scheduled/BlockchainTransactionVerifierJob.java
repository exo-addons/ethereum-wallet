package org.exoplatform.addon.ethereum.wallet.scheduled;

import static org.exoplatform.addon.ethereum.wallet.utils.Utils.EMPTY_HASH;
import static org.exoplatform.addon.ethereum.wallet.utils.Utils.NEW_TRANSACTION_EVENT;

import java.time.Duration;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.web3j.protocol.core.methods.response.Transaction;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.addon.ethereum.wallet.service.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class BlockchainTransactionVerifierJob implements Job {

  private static final Log                 LOG = ExoLogger.getLogger(BlockchainTransactionVerifierJob.class);

  private EthereumWalletService            ethereumWalletService;

  private EthereumClientConnector          ethereumClientConnector;

  private EthereumWalletTransactionService transactionService;

  private ListenerService                  listenerService;

  private ExoContainer                     container;

  public BlockchainTransactionVerifierJob() {
    this(PortalContainer.getInstance());
  }

  public BlockchainTransactionVerifierJob(ExoContainer container) {
    this.container = container;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      GlobalSettings settings = getEthereumWalletService().getSettings();
      if (settings == null || settings.getDefaultNetworkId() == null) {
        LOG.debug("Empty network id on settings, ignore blockchain listening");
        return;
      }
      List<TransactionDetail> pendingTransactions = getPendingTransactions(settings.getDefaultNetworkId());
      if (pendingTransactions != null && !pendingTransactions.isEmpty()) {
        LOG.debug("Checking on blockchain the status of {} transactions marked as pending in database",
                  pendingTransactions.size());
        long pendingTransactionMaxDays = getTransactionService().getPendingTransactionMaxDays();
        for (TransactionDetail pendingTransactionDetail : pendingTransactions) {
          verifyTransactionStatusOnBlockchain(pendingTransactionDetail, pendingTransactionMaxDays);
        }
      }
    } catch (Exception e) {
      LOG.error("Error while checking pending transactions", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  private void verifyTransactionStatusOnBlockchain(TransactionDetail pendingTransactionDetail, long pendingTransactionMaxDays) {
    String hash = pendingTransactionDetail.getHash();
    try {
      Transaction transaction = getEthereumClientConnector().getTransaction(hash);
      String blockHash = transaction == null ? null : transaction.getBlockHash();
      if (!StringUtils.isBlank(blockHash)
          && !StringUtils.equalsIgnoreCase(EMPTY_HASH, blockHash)
          && transaction.getBlockNumber() != null) {
        getListenerService().broadcast(NEW_TRANSACTION_EVENT, transaction, null);
      } else if (pendingTransactionMaxDays > 0) {
        long creationTimestamp = pendingTransactionDetail.getTimestamp();
        if (transaction == null && creationTimestamp > 0) {
          Duration duration = Duration.ofMillis(System.currentTimeMillis() - creationTimestamp);
          if (duration.toDays() >= pendingTransactionMaxDays) {
            LOG.info("Transaction '{}' was not found on blockchain for more than '{}' days, so mark it as failed",
                     hash,
                     pendingTransactionMaxDays);
            getListenerService().broadcast(NEW_TRANSACTION_EVENT, hash, null);
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Error treating pending transaction: {}", hash, e);
    }
  }

  private List<TransactionDetail> getPendingTransactions(long networkId) {
    return getTransactionService().getPendingTransactions(networkId);
  }

  private EthereumWalletService getEthereumWalletService() {
    if (ethereumWalletService == null) {
      ethereumWalletService = CommonsUtils.getService(EthereumWalletService.class);
    }
    return ethereumWalletService;
  }

  private EthereumClientConnector getEthereumClientConnector() {
    if (ethereumClientConnector == null) {
      ethereumClientConnector = CommonsUtils.getService(EthereumClientConnector.class);
    }
    return ethereumClientConnector;
  }

  private EthereumWalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(EthereumWalletTransactionService.class);
    }
    return transactionService;
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }
}
