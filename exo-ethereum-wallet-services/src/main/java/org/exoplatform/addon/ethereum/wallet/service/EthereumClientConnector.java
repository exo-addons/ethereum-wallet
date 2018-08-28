package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.rest.Utils.NEW_TRANSACTION_EVENT;

import java.net.ConnectException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import rx.Subscription;

/**
 * A Web3j connector class to interact with Ethereum Network
 */
public class EthereumClientConnector implements Startable {

  private static final Log      LOG                     = ExoLogger.getLogger(EthereumClientConnector.class);

  private ExoContainer          container;

  private EthereumWalletStorage ethereumWalletStorage;

  private ListenerService       listenerService;

  private String                websocketProviderURL    = null;

  private Subscription          transactionSubscription = null;

  private Queue<Transaction>    queue                   = new ConcurrentLinkedQueue<>();

  public EthereumClientConnector(EthereumWalletStorage ethereumWalletStorage,
                                 ListenerService listenerService,
                                 ExoContainer container) {
    this.ethereumWalletStorage = ethereumWalletStorage;
    this.listenerService = listenerService;
    this.container = container;
  }

  @Override
  public void start() {
    RequestLifeCycle.begin(container);
    try {
      GlobalSettings storedSettings = this.ethereumWalletStorage.getSettings(null, null);
      if (storedSettings != null && StringUtils.isNotBlank(storedSettings.getWebsocketProviderURL())) {
        this.listenToTransactions(storedSettings.getWebsocketProviderURL());
      }
    } catch (ConnectException e) {
      LOG.error("Error connecting to Etheureum network using Websocket, not event listening will be available", e);
    } finally {
      RequestLifeCycle.end();
    }

    // Transactions Queue processing
    Stream.generate(() -> queue.poll()).forEach(transaction -> {
      try {
        if (transaction != null) {
          listenerService.broadcast(NEW_TRANSACTION_EVENT, null, transaction);
        }
      } catch (Exception e) {
        LOG.error("Error while handling transaction", e);
      }
    });
  }

  @Override
  public void stop() {
    unsubscribe();
  }

  /**
   * Add subscription on Ethereum network to listen on all new transactions
   * 
   * @param providerURL
   * @throws ConnectException
   */
  public void listenToTransactions(String providerURL) throws ConnectException {
    unsubscribe();
    Web3j web3j = null;
    if (providerURL.startsWith("ws:") || providerURL.startsWith("wss:")) {
      WebSocketService web3jService = new WebSocketService(providerURL, true);
      web3jService.connect();
      web3j = Web3j.build(web3jService);
    } else {
      web3j = Web3j.build(new HttpService(providerURL));
    }

    this.websocketProviderURL = providerURL;
    transactionSubscription = web3j.transactionObservable().subscribe(tx -> {
      queue.add(tx);
    });
  }

  public void unsubscribe() {
    if (transactionSubscription != null) {
      LOG.info("unsubscribe to Ethereum transaction listener on url {}", this.websocketProviderURL);
      transactionSubscription.unsubscribe();
    }
  }

  public String getWebsocketProviderURL() {
    return websocketProviderURL;
  }

}
