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

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

  private EthereumWalletStorage    ethereumWalletStorage;

  private ListenerService          listenerService;

  private GlobalSettings           globalSettings           = null;

  private Web3j                    web3j                    = null;

  private WebSocketClient          webSocketClient          = null;

  private WebSocketService         web3jService             = null;

  private Subscription             transactionSubscription  = null;

  private Subscription             blockSubscription        = null;

  private Queue<Transaction>       queue                    = new ConcurrentLinkedQueue<>();

  private ScheduledExecutorService scheduledExecutorService = null;

  private long                     lastWatchedBlockNumber   = 0;

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
        this.globalSettings = storedSettings;
        this.lastWatchedBlockNumber = this.ethereumWalletStorage.getLastWatchedBlockNumber(storedSettings.getDefaultNetworkId());
      }
    } catch (Throwable e) {
      LOG.error("Error retrieving global settings", e);
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
        closeConnection();
        return;
      }

      Transaction transaction = queue.poll();
      while (transaction != null) {
        try {
          listenerService.broadcast(NEW_TRANSACTION_EVENT, transaction, null);
        } catch (Throwable e) {
          LOG.warn("Error while handling transaction", e);
        }
        transaction = queue.poll();
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
    this.blockSubscription = web3j.blockObservable(false).subscribe(block -> {
      if (block != null && block.getBlock() != null) {
        try {
          listenerService.broadcast(NEW_BLOCK_EVENT, block.getBlock(), null);
          this.lastWatchedBlockNumber = block.getBlock().getNumber().longValue();
        } catch (Throwable e) {
          LOG.warn("Error while handling block", e);
        }
      }
    });
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
    if (this.blockSubscription != null) {
      try {
        this.blockSubscription.unsubscribe();
      } catch (Throwable e) {
        LOG.warn("Error occurred while unsubscribing to Ethereum block events", e);
      }
      this.blockSubscription = null;
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
    return globalSettings.getWebsocketProviderURL();
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
    GlobalSettings oldGlobalSettings = this.globalSettings;
    this.globalSettings = newGlobalSettings;

    // If web networkId changed, then init last listened block on this network
    if (newGlobalSettings.getDefaultNetworkId() != null
        && !newGlobalSettings.getDefaultNetworkId().equals(oldGlobalSettings.getDefaultNetworkId())) {
      this.lastWatchedBlockNumber = this.ethereumWalletStorage.getLastWatchedBlockNumber(newGlobalSettings.getDefaultNetworkId());
    }

    // If web socket connection changed, then init new connection
    if (newGlobalSettings.getWebsocketProviderURL() != null
        && !StringUtils.equals(newGlobalSettings.getWebsocketProviderURL(), oldGlobalSettings.getWebsocketProviderURL())) {
      closeConnection();
      initWeb3Connection();
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

  private void initWeb3Connection() throws Exception {
    if (getWebsocketProviderURL() == null) {
      throw new IllegalStateException("No configured URL for Ethereum Websocket connection");
    }

    if (web3j == null || webSocketClient.isClosed()) {
      if (web3j != null && webSocketClient.isClosed()) {
        LOG.info("Connection was interrupted, atempt to reconnect to Ethereum network endpoint {}", getWebsocketProviderURL());
      } else {
        LOG.info("Connecting to Ethereum network endpoint {}", getWebsocketProviderURL());
      }

      if (getWebsocketProviderURL().startsWith("ws:") || getWebsocketProviderURL().startsWith("wss:")) {
        stopListeninigToTransactions();
        establishConnection();
        startListeninigToTransactions();
      } else {
        throw new IllegalStateException("Bad format for configured URL " + getWebsocketProviderURL()
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

    webSocketClient = new WebSocketClient(new URI(getWebsocketProviderURL()));
    web3jService = new WebSocketService(webSocketClient, true);

    web3jService.connect();
    if (!webSocketClient.isOpen()) {
      throw new IllegalStateException("Error connecting to " + getWebsocketProviderURL());
    }

    web3j = Web3j.build(web3jService);
    LOG.info("Connection established to Ethereum network endpoint {}", getWebsocketProviderURL());
  }

  /**
   * Test to synchronously test connection, else, it will wait infinitely until
   * a connection is established
   */
  private void testConnection() throws URISyntaxException {
    WebSocketClient webSocketClient = new WebSocketClient(new URI(getWebsocketProviderURL()));
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
      throw new IllegalStateException("Connection failed to " + getWebsocketProviderURL());
    }
  }
}
