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

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.NEW_BLOCK_EVENT;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.NEW_TRANSACTION_EVENT;

import java.io.IOException;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.*;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
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

  private EthereumWalletService    ethereumWalletService;

  private ListenerService          listenerService;

  private GlobalSettings           globalSettings           = null;

  private Web3j                    web3j                    = null;

  private WebSocketClient          webSocketClient          = null;

  private WebSocketService         web3jService             = null;

  private Subscription             transactionSubscription  = null;

  private Queue<Transaction>       queue                    = new ConcurrentLinkedQueue<>();

  private ScheduledExecutorService scheduledExecutorService = null;

  private long                     lastWatchedBlockNumber   = 0;

  private boolean                  initializing             = false;

  public EthereumClientConnector(EthereumWalletService ethereumWalletService,
                                 ListenerService listenerService,
                                 ExoContainer container) {
    this.ethereumWalletService = ethereumWalletService;
    this.listenerService = listenerService;
    this.container = container;
  }

  @Override
  public void start() {
    RequestLifeCycle.begin(container);
    try {
      GlobalSettings storedSettings = this.ethereumWalletService.getSettings();
      if (storedSettings != null && StringUtils.isNotBlank(storedSettings.getWebsocketProviderURL())) {
        this.globalSettings = storedSettings;
        this.lastWatchedBlockNumber = this.ethereumWalletService.getLastWatchedBlockNumber(storedSettings.getDefaultNetworkId());
      }
    } catch (Throwable e) {
      LOG.error("Error retrieving global settings", e);
    } finally {
      RequestLifeCycle.end();
    }

    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-websocket-connector-%d").build();
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);

    // Transactions Queue processing
    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      try {
        if (StringUtils.isBlank(getWebsocketProviderURL())) {
          closeConnection();
          return;
        }
        if (!initWeb3Connection()) {
          LOG.info("Web3 connection initialization in progress, skip transaction processing until it's initialized");
          return;
        }
      } catch (Throwable e) {
        LOG.error("Error while checking connection status to Etherreum Websocket endpoint: {}", e.getMessage());
        closeConnection();
        return;
      }

      Transaction transaction = queue.poll();
      while (transaction != null) {
        try {
          listenerService.broadcast(NEW_TRANSACTION_EVENT, transaction, null);
          if (transaction.getBlockNumber() != null && transaction.getBlockNumber().longValue() > this.lastWatchedBlockNumber) {
            this.lastWatchedBlockNumber = transaction.getBlockNumber().longValue();
          }
        } catch (Throwable e) {
          LOG.warn("Error while handling transaction", e);
        }
        transaction = queue.poll();
      }
      try {
        listenerService.broadcast(NEW_BLOCK_EVENT, this.lastWatchedBlockNumber, null);
      } catch (Throwable e) {
        LOG.warn("Error while broadcasting last watched block number event", e);
      }
    }, 10, 10, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
    closeConnection();
  }

  /**
   * Add subscription on Ethereum network to listen on all new transactions
   * 
   * @throws Exception
   */
  public void startListeninigToTransactions() throws Exception {
    if (this.lastWatchedBlockNumber == 0) {
      LOG.info("Initiate subscription to Ethereum transaction events starting from latest block");
      this.transactionSubscription = web3j.transactionObservable().subscribe(tx -> {
        queue.add(tx);
      });
    } else {
      LOG.info("Initiate subscription to Ethereum transaction events starting from block {}", this.lastWatchedBlockNumber);
      DefaultBlockParameterNumber startBlock = new DefaultBlockParameterNumber(this.lastWatchedBlockNumber);
      this.transactionSubscription = web3j.catchUpToLatestAndSubscribeToNewTransactionsObservable(startBlock).subscribe(tx -> {
        queue.add(tx);
      });
    }
  }

  /**
   * Unsubscribe from Ethereum network transaction events
   */
  public void stopListeninigToTransactions() {
    if (this.transactionSubscription != null) {
      LOG.info("unsubscribe to Ethereum transaction listener on url {}", getWebsocketProviderURL());
      try {
        this.transactionSubscription.unsubscribe();
      } catch (Throwable e) {
        LOG.warn("Error occurred while unsubscribing to Ethereum transaction events", e);
      }
      this.transactionSubscription = null;
    }
  }

  /**
   * Get transaction receipt
   * 
   * @param transactionHash
   * @return
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public TransactionReceipt getTransactionReceipt(String transactionHash) throws Exception {
    EthGetTransactionReceipt ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get();
    if (ethGetTransactionReceipt != null && ethGetTransactionReceipt.getResult() != null) {
      return ethGetTransactionReceipt.getResult();
    }
    return null;
  }

  /**
   * Returns used Websocket URL
   * 
   * @return
   */
  public String getWebsocketProviderURL() {
    GlobalSettings settings = ethereumWalletService.getSettings();
    return settings == null ? null : settings.getWebsocketProviderURL();
  }

  /**
   * Change currently used globalSettings and re-init
   * 
   * @param newGlobalSettings
   * @throws Exception
   */
  public void changeSettings(GlobalSettings newGlobalSettings) throws Exception {
    if (newGlobalSettings == null) {
      throw new IllegalArgumentException("GlobalSettings argument is mandatory");
    }
    if (this.globalSettings == null) {
      // not yet initialized
      return;
    }
    GlobalSettings oldGlobalSettings = this.globalSettings;
    this.globalSettings = newGlobalSettings;

    // If web networkId changed, then init last listened block on this network
    if (newGlobalSettings.getDefaultNetworkId() != null
        && !newGlobalSettings.getDefaultNetworkId().equals(oldGlobalSettings.getDefaultNetworkId())) {
      this.lastWatchedBlockNumber = this.ethereumWalletService.getLastWatchedBlockNumber(newGlobalSettings.getDefaultNetworkId());
    }

    // If web socket connection changed, then init new connection
    if (StringUtils.isBlank(newGlobalSettings.getWebsocketProviderURL())
        || !StringUtils.equals(newGlobalSettings.getWebsocketProviderURL(), oldGlobalSettings.getWebsocketProviderURL())) {
      closeConnection();
    }
  }

  public long getLastWatchedBlockNumber() {
    return lastWatchedBlockNumber;
  }

  public void setLastWatchedBlockNumber(long lastWatchedBlockNumber) {
    this.lastWatchedBlockNumber = lastWatchedBlockNumber;
  }

  private void closeConnection() {
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
  }

  private boolean initWeb3Connection() throws Exception {
    if (initializing) {
      return false;
    }
    initializing = true;
    try {
      if (getWebsocketProviderURL() == null) {
        throw new IllegalStateException("No configured URL for Ethereum Websocket connection");
      }

      if (this.web3j == null || this.webSocketClient == null || this.webSocketClient.isClosed()
          || this.transactionSubscription == null || this.transactionSubscription.isUnsubscribed()) {

        if (getWebsocketProviderURL().startsWith("ws:") || getWebsocketProviderURL().startsWith("wss:")) {
          stopListeninigToTransactions();
          establishConnection();
          startListeninigToTransactions();
        } else {
          throw new IllegalStateException("Bad format for configured URL " + getWebsocketProviderURL()
              + " for Ethereum Websocket connection");
        }
      }
      return true;
    } finally {
      initializing = false;
    }
  }

  private void establishConnection() throws Exception {
    if (web3jService != null) {
      web3jService.close();
      web3jService = null;
    }

    if (web3j != null) {
      LOG.info("Connection was interrupted, atempt to reconnect to Ethereum network endpoint {}", getWebsocketProviderURL());
    } else {
      LOG.info("Connecting to Ethereum network endpoint {}", getWebsocketProviderURL());
    }

    web3jService = testConnection();
    web3j = Web3j.build(web3jService);
    LOG.info("Connection established to Ethereum network endpoint {}", getWebsocketProviderURL());
  }

  /**
   * Test to synchronously test connection, else, it will wait infinitely until
   * a connection is established
   */
  private WebSocketService testConnection() throws Exception {
    try {
      this.webSocketClient = new WebSocketClient(new URI(getWebsocketProviderURL()));
      this.webSocketClient.setConnectionLostTimeout(10);
      this.web3jService = new WebSocketService(webSocketClient, true);
      this.webSocketClient.setListener(new WebSocketListener() {
        @Override
        public void onMessage(String message) throws IOException {
        }

        @Override
        public void onError(Exception e) {
          LOG.error("Connection failed to " + getWebsocketProviderURL());
        }

        @Override
        public void onClose() {
        }
      });
      this.web3jService.connect();
      Thread.sleep(10000);
      if (!this.webSocketClient.isOpen()) {
        closeConnectionAndThrowError(null);
      }
    } catch (Throwable e) {
      closeConnectionAndThrowError(e);
    }
    return this.web3jService;
  }

  private void closeConnectionAndThrowError(Throwable e) {
    try {
      this.web3jService.close();
    } finally {
      this.webSocketClient = null;
      this.web3jService = null;
    }
    if (e == null) {
      throw new IllegalStateException("Connection failed to " + getWebsocketProviderURL());
    } else {
      throw new IllegalStateException("Connection failed to " + getWebsocketProviderURL(), e);
    }
  }
}
