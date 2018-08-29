/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.NEW_TRANSACTION_EVENT;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.setTimeout;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.websocket.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

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

  private static final Log         LOG                      = ExoLogger.getLogger(EthereumClientConnector.class);

  private ExoContainer             container;

  private EthereumWalletStorage    ethereumWalletStorage;

  private ListenerService          listenerService;

  private String                   websocketProviderURL     = null;

  private Web3j                    web3j                    = null;

  private WebSocketClient          webSocketClient          = null;

  private WebSocketService         web3jService             = null;

  private Subscription             transactionSubscription  = null;

  private Queue<Transaction>       queue                    = new ConcurrentLinkedQueue<>();

  private ScheduledExecutorService scheduledExecutorService = null;

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
        this.websocketProviderURL = storedSettings.getWebsocketProviderURL();
      }
    } catch (Throwable e) {
      LOG.error("Error connecting to Etheureum network using Websocket, not event listening will be available", e);
    } finally {
      RequestLifeCycle.end();
    }

    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-websocket-connector-%d").build();
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);

    // Transactions Queue processing
    scheduledExecutorService.scheduleAtFixedRate(() -> {
      try {
        initWeb3Connection();
      } catch (Throwable e) {
        LOG.error("Error while checking connection status to Etherreum Websocket endpoint", e);
        return;
      }

      Transaction transaction = queue.poll();
      while (transaction != null) {
        try {
          EthGetTransactionReceipt ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transaction.getHash()).sendAsync().get();
          TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().get();

          listenerService.broadcast(NEW_TRANSACTION_EVENT, transaction, transactionReceipt);
        } catch (Throwable e) {
          LOG.error("Error while handling transaction", e);
        }
        transaction = queue.poll();
      }
    }, 10, 10, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
    stopListeninigToTransactions();
    if (web3jService != null) {
      web3jService.close();
    }
  }

  /**
   * Add subscription on Ethereum network to listen on all new transactions
   * 
   * @throws Exception
   */
  public void startListeninigToTransactions() throws Exception {
    LOG.info("Initiate subscription to Ethereum transaction events");
    this.transactionSubscription = web3j.transactionObservable().subscribe(tx -> {
      queue.add(tx);
    });
  }

  /**
   * Unsubscribe from Ethereum network transaction events
   */
  public void stopListeninigToTransactions() {
    if (this.transactionSubscription != null) {
      LOG.info("unsubscribe to Ethereum transaction listener on url {}", this.websocketProviderURL);
      try {
        this.transactionSubscription.unsubscribe();
      } catch (Throwable e) {
        LOG.warn("Error occurred while unsubscribing to Ethereum transaction events", e);
      }
      this.transactionSubscription = null;
    }
  }

  /**
   * Returns used Websocket URL
   * 
   * @return
   */
  public String getWebsocketProviderURL() {
    return websocketProviderURL;
  }

  /**
   * Change currently used websocketProviderURL and restart listening to
   * transactions
   * 
   * @param websocketProviderURL
   * @throws Exception
   */
  public void changeWebsocketProviderURL(String websocketProviderURL) throws Exception {
    this.websocketProviderURL = websocketProviderURL;
    stopListeninigToTransactions();
    if (web3jService != null) {
      if (webSocketClient != null && webSocketClient.isOpen()) {
        try {
          web3jService.close();
        } catch (Throwable e) {
          LOG.warn("Error closing old websocket connection", e);
        }
      }
      web3j = null;
      web3jService = null;
      webSocketClient = null;
    }
    initWeb3Connection();
  }

  private void initWeb3Connection() throws Exception {
    if (this.websocketProviderURL == null) {
      throw new IllegalStateException("No configured URL for Ethereum Websocket connection");
    }

    if (web3j == null || webSocketClient.isClosed()) {
      if (web3j != null && webSocketClient.isClosed()) {
        LOG.info("Connection was interrupted, atempt to reconnect to Ethereum network endpoint {}", this.websocketProviderURL);
      } else {
        LOG.info("Connecting to Ethereum network endpoint {}", this.websocketProviderURL);
      }

      if (this.websocketProviderURL.startsWith("ws:") || this.websocketProviderURL.startsWith("wss:")) {
        stopListeninigToTransactions();
        establishConnection();
        startListeninigToTransactions();
      } else {
        throw new IllegalStateException("Bad format for configured URL " + this.websocketProviderURL
            + " for Ethereum Websocket connection");
      }
    }
  }

  private void establishConnection() throws Exception {
    if (web3jService != null) {
      web3jService.close();
      web3jService = null;
    }

    testConnection();

    webSocketClient = new WebSocketClient(new URI(this.websocketProviderURL));
    webSocketClient.setConnectionLostTimeout(10);
    web3jService = new WebSocketService(webSocketClient, true);

    web3jService.connect();
    if (!webSocketClient.isOpen()) {
      throw new IllegalStateException("Error connecting to " + this.websocketProviderURL);
    }

    web3j = Web3j.build(web3jService);
    LOG.info("Conection established to Ethereum network endpoint {}", this.websocketProviderURL);
  }

  /**
   * Test to synchronously test connection, else, it will wait infinitely until
   * a connection is established
   */
  private void testConnection() throws URISyntaxException {
    WebSocketClient webSocketClient = new WebSocketClient(new URI(this.websocketProviderURL));
    webSocketClient.setListener(new WebSocketListener() {
      @Override
      public void onMessage(String message) throws IOException {
      }

      @Override
      public void onError(Exception e) {
      }

      @Override
      public void onClose() {
      }
    });

    AtomicBoolean connected = new AtomicBoolean(false);
    // close connection after 10 seconds to not wait indefinitely
    setTimeout(() -> {
      connected.set(webSocketClient.isOpen());
      webSocketClient.close();
    }, 10000);
    webSocketClient.run();
    if (!connected.get()) {
      throw new IllegalStateException("Connection failed to " + this.websocketProviderURL);
    }
  }
}
