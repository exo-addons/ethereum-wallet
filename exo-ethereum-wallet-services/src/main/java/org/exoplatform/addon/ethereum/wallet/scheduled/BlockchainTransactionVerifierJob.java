package org.exoplatform.addon.ethereum.wallet.scheduled;

import static org.exoplatform.addon.ethereum.wallet.utils.Utils.EMPTY_HASH;
import static org.exoplatform.addon.ethereum.wallet.utils.Utils.NEW_TRANSACTION_EVENT;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.web3j.protocol.core.methods.response.Transaction;

import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.addon.ethereum.wallet.service.EthereumClientConnector;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletTransactionService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class BlockchainTransactionVerifierJob implements Job {

  private static final Log                 LOG = ExoLogger.getLogger(BlockchainTransactionVerifierJob.class);

  private EthereumClientConnector          ethereumClientConnector;

  private EthereumWalletTransactionService transactionService;

  private ListenerService                  listenerService;

  private ExoContainer                     container;

  public BlockchainTransactionVerifierJob() {
    this(PortalContainer.getInstance());
  }

  public BlockchainTransactionVerifierJob(ExoContainer exoContainer) {
    this.container = exoContainer;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    try {
      List<TransactionDetail> pendingTransactions = getPendingTransactions();
      if (pendingTransactions != null && !pendingTransactions.isEmpty()) {
        LOG.debug("Checking on blockchain the status of {} transactions marked as pending in database",
                  pendingTransactions.size());
        for (TransactionDetail transactionDetail : pendingTransactions) {
          String hash = transactionDetail.getHash();
          try { // NOSONAR
            Transaction transaction = getEthereumClientConnector().getTransaction(hash);
            String blockHash = transaction == null ? null : transaction.getBlockHash();
            if (!StringUtils.isBlank(blockHash)
                && !StringUtils.equalsIgnoreCase(EMPTY_HASH, blockHash)
                && transaction.getBlockNumber() != null) {
              getListenerService().broadcast(NEW_TRANSACTION_EVENT, transaction, null);
            }
          } catch (Exception e) {
            LOG.warn("Error treating pending transaction: {}", hash, e);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Error while checking pending transactions", e);
    } finally {
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  private List<TransactionDetail> getPendingTransactions() {
    return getTransactionService().getPendingTransactions();
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
