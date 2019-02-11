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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult;
import org.web3j.protocol.websocket.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.model.MinedTransactionDetail;
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
 * A Web3j connector class to interact with Ethereum Blockchain
 */
@ManagedBy(EthereumClientConnectorManaged.class)
public class EthereumClientConnector implements Startable {

  private static final String           ERROR_CLOSING_WEB_SOCKET_MESSAGE = "Error closing web socket";

  private static final Log              LOG                              = ExoLogger.getLogger(EthereumClientConnector.class);

  private ExoContainer                  container;

  private EthereumWalletService         ethereumWalletService;

  private ListenerService               listenerService;

  private GlobalSettings                globalSettings                   = null;

  private Web3j                         web3j                            = null;

  private WebSocketClient               webSocketClient                  = null;

  private WebSocketService              web3jService                     = null;

  private Subscription                  transactionSubscription          = null;

  private Queue<MinedTransactionDetail> transactionHashesQueue           = new ConcurrentLinkedQueue<>();

  private ScheduledExecutorService      scheduledExecutorService         = null;

  private long                          lastWatchedBlockNumber           = 0;

  private long                          lastBlockNumberOnStartupTime     = 0;

  private int                           transactionsCountPerBlock        = 0;

  private boolean                       initializing                     = false;

  private int                           connectionInterruptionCount      = -1;

  private long                          watchingBlockchainStartTime      = 0;

  private int                           watchedTransactionCount          = 0;

  private int                           transactionQueueMaxSize          = 0;

  private boolean                       stopping                         = false;

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
        if (this.stopping) {
          LOG.info("Stopping server, thus no new connection is attempted again");
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

      MinedTransactionDetail minedTransactionDetail = transactionHashesQueue.poll();
      while (minedTransactionDetail != null) {
        try {
          getListenerService().broadcast(NEW_TRANSACTION_EVENT, minedTransactionDetail, null);
        } catch (Throwable e) {
          LOG.warn("Error while handling transaction", e);
        }
        minedTransactionDetail = transactionHashesQueue.poll();
      }
    }, 10, 10, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    this.stopping = true;
    scheduledExecutorService.shutdown();
    closeConnection();
  }

  /**
   * Add subscription on Ethereum network to listen on all new transactions
   */
  public void startListeninigToTransactions() {
    if (this.lastWatchedBlockNumber == 0) {
      LOG.info("Initiate subscription to Ethereum transaction events starting from latest block");
      this.transactionSubscription = web3j.blockObservable(false).subscribe(tx -> addBlockTransactionsToQueue(tx));
    } else {
      LOG.info("Initiate subscription to Ethereum transaction events starting from block {}", this.lastWatchedBlockNumber);
      DefaultBlockParameterNumber startBlock = new DefaultBlockParameterNumber(this.lastWatchedBlockNumber);
      this.transactionSubscription = web3j.catchUpToLatestAndSubscribeToNewBlocksObservable(startBlock, false)
                                          .subscribe(block -> addBlockTransactionsToQueue(block));
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
   * Get transaction by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction object
   * @throws InterruptedException when Blockchain request is interrupted
   */
  public Transaction getTransaction(String transactionHash) throws InterruptedException {
    waitConnection();
    EthTransaction ethTransaction;
    try {
      ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();
    } catch (IOException e) {
      LOG.info("Connection interrupted while getting Transaction '{}' information. Reattempt until getting it. Reason: {}",
               transactionHash,
               e.getMessage());
      return getTransaction(transactionHash);
    }
    if (ethTransaction != null) {
      return ethTransaction.getResult();
    }
    return null;
  }

  /**
   * Get block by hash
   * 
   * @param blockHash block hash to retrieve
   * @return Web3j Block object
   * @throws InterruptedException when Blockchain request is interrupted
   */
  public Block getBlock(String blockHash) throws InterruptedException {
    waitConnection();
    EthBlock ethBlock;
    try {
      ethBlock = web3j.ethGetBlockByHash(blockHash, false).send();
    } catch (IOException e) {
      LOG.info("Connection interrupted while getting Block '{}' information. Reattempt until getting it. Reason: {}",
               blockHash,
               e.getMessage());
      return getBlock(blockHash);
    }
    if (ethBlock != null && ethBlock.getResult() != null) {
      return ethBlock.getResult();
    }
    return null;
  }

  /**
   * Get transaction receipt by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction receipt object
   * @throws InterruptedException when Blockchain request is interrupted
   */
  public TransactionReceipt getTransactionReceipt(String transactionHash) throws InterruptedException {
    waitConnection();
    EthGetTransactionReceipt ethGetTransactionReceipt;
    try {
      ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).send();
    } catch (IOException e) {
      LOG.info("Connection interrupted while getting Transaction receipt '{}' information. Reattempt until getting it. Reason: {}",
               transactionHash,
               e.getMessage());
      return getTransactionReceipt(transactionHash);
    }
    if (ethGetTransactionReceipt != null) {
      return ethGetTransactionReceipt.getResult();
    }
    return null;
  }

  /**
   * Returns used Websocket URL
   * 
   * @return blockchain web socket endpoint URL
   */
  public String getWebsocketProviderURL() {
    GlobalSettings settings = ethereumWalletService.getSettings();
    return settings == null ? null : settings.getWebsocketProviderURL();
  }

  /**
   * Change currently used globalSettings and re-init
   * 
   * @param newGlobalSettings newly saved global settings
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

  /**
   * @return true if the connection to the blockchain is established
   */
  public boolean isConnected() {
    return web3j != null && web3jService != null && webSocketClient != null && webSocketClient.isOpen();
  }

  /**
   * @return last wtahced block number
   */
  public long getLastWatchedBlockNumber() {
    return lastWatchedBlockNumber;
  }

  /**
   * @return number of blockchain connection interruption
   */
  public int getConnectionInterruptionCount() {
    return connectionInterruptionCount;
  }

  /**
   * @param lastWatchedBlockNumber last watched block number
   */
  public void setLastWatchedBlockNumber(long lastWatchedBlockNumber) {
    this.lastWatchedBlockNumber = lastWatchedBlockNumber;
  }

  /**
   * @return transaction queue currently processing size
   */
  public int getTransactionQueueSize() {
    return transactionHashesQueue.size();
  }

  /**
   * @return timestamp in milliseconds of blockchaing watching start time
   */
  public long getWatchingBlockchainStartTime() {
    return watchingBlockchainStartTime;
  }

  /**
   * @return number of treated watched transactions count
   */
  public int getWatchedTransactionCount() {
    return watchedTransactionCount;
  }

  /**
   * @return max treating transactions queue size
   */
  public int getTransactionQueueMaxSize() {
    return transactionQueueMaxSize;
  }

  /**
   * @return block number on blockchain in stratup time
   */
  public long getLastBlockNumberOnStartupTime() {
    return lastBlockNumberOnStartupTime;
  }

  /**
   * @return estimation of transactions count per block
   */
  public int getTransactionsCountPerBlock() {
    return transactionsCountPerBlock;
  }

  private void waitConnection() throws InterruptedException {
    if (this.stopping) {
      throw new IllegalStateException("Server is stopping, thus not Web3 request should be emitted");
    }
    while (!isConnected()) {
      LOG.info("Wait until Websocket connection to blockchain is established to retrieve information");
      Thread.sleep(5000);
    }
  }

  @SuppressWarnings("rawtypes")
  private void addBlockTransactionsToQueue(EthBlock ethBlock) {
    final Block block = ethBlock.getBlock();
    this.lastWatchedBlockNumber = block.getNumber().longValue();
    List<TransactionResult> transactions = block.getTransactions();
    transactionHashesQueue.addAll(transactions.parallelStream()
                                              .map(transaction -> new MinedTransactionDetail(transaction.get().toString(),
                                                                                             block.getTimestamp().longValue()))
                                              .collect(Collectors.toList()));
    watchedTransactionCount += transactions.size();
    transactionQueueMaxSize = Math.max(transactionHashesQueue.size() + 1, transactionQueueMaxSize);
    try {
      getListenerService().broadcast(NEW_BLOCK_EVENT, this.lastWatchedBlockNumber, null);
    } catch (Throwable e) {
      LOG.warn("Error while broadcasting last watched block number event", e);
    }
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
    if (initializing || this.stopping) {
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
