package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.EMPTY_HASH;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.NEW_TRANSACTION_EVENT;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
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

  private ScheduledFuture<?>               scheduledFuture;

  private Runnable                         transactionVerifierRunnable;

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
    transactionVerifierRunnable = () -> {
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(container);
      try {
        LOG.debug("Checking transactions with not sent notifications");
        List<TransactionDetail> pendingTransactions = getPendingTransactions();
        if (pendingTransactions != null && !pendingTransactions.isEmpty()) {
          LOG.info("Checking {} pending transactions if it has not been notified to users", pendingTransactions.size());
          for (TransactionDetail transactionDetail : pendingTransactions) {
            String hash = transactionDetail.getHash();
            try { // NOSONAR
              Transaction transaction = ethereumClientConnector.getTransaction(hash);
              if (transaction != null
                  && !StringUtils.isBlank(transaction.getBlockHash())
                  && !StringUtils.equals(EMPTY_HASH, transaction.getBlockHash())
                  && transaction.getBlockNumber() != null
                  && transaction.getBlockNumber().longValue() < ethereumClientConnector.getLastWatchedBlockNumber()
                  && transactionDetail.isPending()) {
                LOG.info("Treating transaction {} that is still marked pending while it has been already mined.", hash);
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
        RequestLifeCycle.end();
      }
    };

    scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(transactionVerifierRunnable, 1, 5, TimeUnit.MINUTES);
  }

  @Override
  public void stop() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
    scheduledExecutorService.shutdown();
  }

  public void runNow() {
    if (scheduledExecutorService != null && transactionVerifierRunnable != null) {
      scheduledExecutorService.execute(transactionVerifierRunnable);
    }
  }

  public Set<String> getPendingTransactionHashes() {
    List<TransactionDetail> pendingTransactions = getPendingTransactions();
    if (pendingTransactions == null || pendingTransactions.isEmpty()) {
      return Collections.emptySet();
    }
    return pendingTransactions.stream().map(transactionDetail -> transactionDetail.getHash()).collect(Collectors.toSet());
  }

  private List<TransactionDetail> getPendingTransactions() {
    return transactionService.getPendingTransactions();
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }
}
