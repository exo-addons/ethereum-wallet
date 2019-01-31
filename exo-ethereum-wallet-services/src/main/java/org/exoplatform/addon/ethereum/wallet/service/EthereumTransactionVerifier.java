package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.NEW_TRANSACTION_EVENT;

import java.util.List;
import java.util.concurrent.*;

import org.picocontainer.Startable;
import org.web3j.protocol.core.methods.response.Transaction;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumTransactionVerifier implements Startable {

  private static final Log                 LOG                      =
                                               ExoLogger.getLogger(EthereumTransactionVerifier.class);

  private ExoContainer                     container;

  private EthereumClientConnector          ethereumClientConnector;

  private EthereumWalletTransactionService transactionService;

  private ListenerService                  listenerService;

  private ScheduledExecutorService         scheduledExecutorService = null;

  public EthereumTransactionVerifier(ExoContainer container,
                                     EthereumClientConnector ethereumClientConnector,
                                     EthereumWalletTransactionService transactionService) {
    this.container = container;
    this.ethereumClientConnector = ethereumClientConnector;
    this.transactionService = transactionService;
  }

  @Override
  public void start() {
    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-transaction-verifier-%d").build();
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);

    // Transactions Queue processing
    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(container);
      try {
        LOG.debug("Checking transactions with not sent notifications");
        List<TransactionDetail> pendingTransactions = transactionService.getPendingTransactions();
        if (pendingTransactions != null && !pendingTransactions.isEmpty()) {
          LOG.info("Checking {} pending transactions if it has not been notified to users");
          for (TransactionDetail transactionDetail : pendingTransactions) {
            String hash = transactionDetail.getHash();
            try { // NOSONAR
              Transaction transaction = ethereumClientConnector.getTransaction(hash);
              if (transaction != null
                  && transaction.getBlockNumber() != null
                  && transaction.getBlockNumber().longValue() < ethereumClientConnector.getLastWatchedBlockNumber()
                  && transactionDetail.isPending()) {
                LOG.info("Treating transaction {} that is still marked pending while it has been already mined.", hash);
                getListenerService().broadcast(NEW_TRANSACTION_EVENT, transaction, null);
              }
            } catch (Exception e) {
              LOG.warn("Error treating pending transaction: {}", hash);
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Error while checking pending transactions", e);
      } finally {
        RequestLifeCycle.end();
      }
    }, 1, 5, TimeUnit.MINUTES);
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdownNow();
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }
}
