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
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.websocket.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.service.managed.EthereumClientConnectorManaged;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.management.annotations.ManagedBy;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import rx.Subscription;

/**
 * A Web3j connector class to interact with Ethereum Network
 */
@ManagedBy(EthereumClientConnectorManaged.class)
public class EthereumClientConnector implements Startable {

  private static final String      ERROR_CLOSING_WEB_SOCKET_MESSAGE = "Error closing web socket";

  private static final Log         LOG                              = ExoLogger.getLogger(EthereumClientConnector.class);

  private ExoContainer             container;

  private EthereumWalletService    ethereumWalletService;

  private ListenerService          listenerService;

  private GlobalSettings           globalSettings                   = null;

  private Web3j                    web3j                            = null;

  private WebSocketClient          webSocketClient                  = null;

  private WebSocketService         web3jService                     = null;

  private Subscription             transactionSubscription          = null;

  private Queue<Transaction>       transactionQueue                 = new ConcurrentLinkedQueue<>();

  private ScheduledExecutorService scheduledExecutorService         = null;

  private long                     lastWatchedBlockNumber           = 0;

  private long                     lastBlockNumberOnStartupTime     = 0;

  private int                      transactionsCountPerBlock        = 0;

  private boolean                  initializing                     = false;

  private int                      connectionInterruptionCount      = -1;

  private long                     watchingBlockchainStartTime      = 0;

  private int                      watchedTransactionCount          = 0;

  private int                      transactionQueueMaxSize          = 0;

  public EthereumClientConnector(EthereumWalletService ethereumWalletService,
                                 ExoContainer container) {
    this.ethereumWalletService = ethereumWalletService;
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
        LOG.warn("Error while checking connection status to Etherreum Websocket endpoint: {}", e.getMessage());
        closeConnection();
        return;
      }

      Transaction transaction = transactionQueue.poll();
      while (transaction != null) {
        try {
          getListenerService().broadcast(NEW_TRANSACTION_EVENT, transaction, null);

          if (transaction.getBlockNumber() != null && transaction.getBlockNumber().longValue() > this.lastWatchedBlockNumber) {
            this.lastWatchedBlockNumber = transaction.getBlockNumber().longValue();
          }
        } catch (Throwable e) {
          LOG.warn("Error while handling transaction", e);
        }
        transaction = transactionQueue.poll();
      }
      try {
        getListenerService().broadcast(NEW_BLOCK_EVENT, this.lastWatchedBlockNumber, null);
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
   */
  public void startListeninigToTransactions() {
    if (this.lastWatchedBlockNumber == 0) {
      LOG.info("Initiate subscription to Ethereum transaction events starting from latest block");
      this.transactionSubscription = web3j.transactionObservable().subscribe(tx -> addTransactionToQueue(tx));
    } else {
      LOG.info("Initiate subscription to Ethereum transaction events starting from block {}", this.lastWatchedBlockNumber);
      DefaultBlockParameterNumber startBlock = new DefaultBlockParameterNumber(this.lastWatchedBlockNumber);
      this.transactionSubscription = web3j.catchUpToLatestAndSubscribeToNewTransactionsObservable(startBlock)
                                          .subscribe(tx -> addTransactionToQueue(tx));
    }
    if (watchingBlockchainStartTime == 0) {
      watchingBlockchainStartTime = System.currentTimeMillis();
    }

    if (this.lastBlockNumberOnStartupTime == 0) {
      try {
        Block block = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();
        this.lastBlockNumberOnStartupTime = block.getNumber().intValue();
        this.transactionsCountPerBlock = block.getTransactions().size();
      } catch (Throwable e) {
        LOG.warn("Error while getting latest block number", e);
      }
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
        LOG.warn("Error occurred while unsubscribing to Ethereum transaction events: {}", e.getMessage());
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
  public TransactionReceipt getTransactionReceipt(String transactionHash) throws InterruptedException, ExecutionException {
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
   */
  public void changeSettings(GlobalSettings newGlobalSettings) {
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

  public int getConnectionInterruptionCount() {
    return connectionInterruptionCount;
  }

  public void setLastWatchedBlockNumber(long lastWatchedBlockNumber) {
    this.lastWatchedBlockNumber = lastWatchedBlockNumber;
  }

  public int getTransactionQueueSize() {
    return transactionQueue.size();
  }

  public long getWatchingBlockchainStartTime() {
    return watchingBlockchainStartTime;
  }

  public int getWatchedTransactionCount() {
    return watchedTransactionCount;
  }

  public int getTransactionQueueMaxSize() {
    return transactionQueueMaxSize;
  }

  public long getLastBlockNumberOnStartupTime() {
    return lastBlockNumberOnStartupTime;
  }

  public int getTransactionsCountPerBlock() {
    return transactionsCountPerBlock;
  }

  private boolean addTransactionToQueue(Transaction tx) {
    watchedTransactionCount++;
    transactionQueueMaxSize = Math.max(transactionQueue.size() + 1, transactionQueueMaxSize);
    return transactionQueue.add(tx);
  }

  private void closeConnection() {
    stopListeninigToTransactions();
    if (web3j != null) {
      try {
        web3j.shutdown();
      } catch (Throwable e) {
        LOG.warn("Error closing old web3j connection: {}", e.getMessage());
        if (this.web3jService != null && webSocketClient != null && webSocketClient.isOpen()) {
          try {
            web3jService.close();
          } catch (Throwable e1) {
            LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
          }
        } else if (webSocketClient != null && webSocketClient.isOpen()) {
          try {
            webSocketClient.close();
          } catch (Throwable e1) {
            LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
          }
        }
      }
      web3j = null;
      web3jService = null;
      webSocketClient = null;
    }
  }

  private boolean initWeb3Connection() {
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
          connectionInterruptionCount++;
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

  private void establishConnection() {
    closeConnection();

    LOG.info("Connecting to Ethereum network endpoint {}", getWebsocketProviderURL());

    web3jService = testConnection();
    web3j = Web3j.build(web3jService);
    LOG.info("Connection established to Ethereum network endpoint {}", getWebsocketProviderURL());
  }

  /**
   * Test to synchronously test connection, else, it will wait infinitely until
   * a connection is established
   */
  private WebSocketService testConnection() {
    try {
      this.webSocketClient = new WebSocketClient(new URI(getWebsocketProviderURL()));
      this.webSocketClient.setConnectionLostTimeout(10);
      this.web3jService = new WebSocketService(webSocketClient, true);
      this.webSocketClient.setListener(new WebSocketListener() {
        @Override
        public void onMessage(String message) throws IOException {
          LOG.debug("A new message is received in testConnection method");
        }

        @Override
        public void onError(Exception e) {
          LOG.warn(getConnectionFailedMessage());
        }

        @Override
        public void onClose() {
          LOG.debug("Websocket connection closed for testConnection method");
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
    if (this.web3j != null) {
      try {
        this.web3j.shutdown();
      } catch (Exception e1) {
        LOG.debug(ERROR_CLOSING_WEB_SOCKET_MESSAGE, e1);
      } finally {
        this.web3j = null;
        this.webSocketClient = null;
        this.web3jService = null;
      }
    }
    if (this.web3jService != null) {
      try {
        this.web3jService.close();
      } catch (Exception e1) {
        LOG.debug(ERROR_CLOSING_WEB_SOCKET_MESSAGE, e1);
      } finally {
        this.web3jService = null;
        this.webSocketClient = null;
      }
    }
    if (this.webSocketClient != null && this.webSocketClient.isOpen()) {
      try {
        this.webSocketClient.close();
      } catch (Exception e1) {
        LOG.debug(ERROR_CLOSING_WEB_SOCKET_MESSAGE, e1);
      } finally {
        this.web3j = null;
        this.webSocketClient = null;
        this.web3jService = null;
      }
    }
    if (e == null) {
      throw new IllegalStateException(getConnectionFailedMessage());
    } else {
      throw new IllegalStateException(getConnectionFailedMessage(), e);
    }
  }

  protected String getConnectionFailedMessage() {
    return "Connection failed to " + getWebsocketProviderURL();
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }
}
