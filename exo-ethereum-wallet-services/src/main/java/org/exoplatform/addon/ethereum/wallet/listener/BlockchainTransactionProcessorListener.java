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
package org.exoplatform.addon.ethereum.wallet.listener;

import org.apache.commons.lang3.StringUtils;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.exoplatform.addon.ethereum.wallet.model.MinedTransactionDetail;
import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.addon.ethereum.wallet.service.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A listener to process newly detected transactions coming from configured
 * network
 */
public class BlockchainTransactionProcessorListener extends Listener<Object, TransactionReceipt> {

  private static final Log           LOG = ExoLogger.getLogger(BlockchainTransactionProcessorListener.class);

  private WalletTransactionService   transactionService;

  private EthereumTransactionDecoder ethereumTransactionDecoder;

  private EthereumClientConnector    ethereumClientConnector;

  private ExoContainer               container;

  public BlockchainTransactionProcessorListener(PortalContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<Object, TransactionReceipt> event) throws Exception {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      Object source = event.getSource();
      if (source == null) {
        return;
      }

      String transactionHash = null;
      String blockHash = null;
      Long blockTimestamp = null;
      if (source instanceof MinedTransactionDetail) {
        MinedTransactionDetail transaction = (MinedTransactionDetail) source;
        transactionHash = transaction.getHash();
        blockHash = transaction.getBlockHash();
        blockTimestamp = transaction.getBlockTimestamp();
      } else if (source instanceof Transaction) {
        Transaction transaction = (Transaction) source;
        transactionHash = transaction.getHash();
        blockHash = transaction.getBlockHash();
      } else {
        transactionHash = (String) source;
      }

      if (StringUtils.isBlank(transactionHash)) {
        LOG.warn("Transaction hash is empty");
        return;
      }

      TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(transactionHash);
      if (transactionDetail == null) {
        LOG.warn("Transaction detail with hash {} wasn't found in database", transactionHash);
        return;
      }

      TransactionReceipt transactionReceipt = event.getData();
      if (transactionReceipt == null) {
        transactionReceipt = getTransactionReceipt(transactionHash);
      }
      boolean broadcastSavingTransaction = transactionDetail.isPending();
      transactionDetail.setPending(false);
      transactionDetail.setSucceeded(transactionReceipt != null && transactionReceipt.isStatusOK());

      // Ensure that stored transaction has a timestamp
      if (transactionDetail.getTimestamp() == 0) {
        if (blockTimestamp != null) {
          transactionDetail.setTimestamp(blockTimestamp);
        } else if (StringUtils.isNotBlank(blockHash)) {
          Block block = getEthereumClientConnector().getBlock(blockHash);
          transactionDetail.setTimestamp(block.getTimestamp().longValue());
        }
      }

      // Ensure that all fields are computed correctly
      getTransactionDecoderService().computeContractTransactionDetail(transactionDetail, transactionReceipt);

      getTransactionService().saveTransactionDetail(transactionDetail, broadcastSavingTransaction);
    } finally {
      RequestLifeCycle.end();
    }
  }

  private TransactionReceipt getTransactionReceipt(String transactionHash) throws InterruptedException {
    TransactionReceipt transactionReceipt = getEthereumClientConnector().getTransactionReceipt(transactionHash);
    if (transactionReceipt == null || "0x0".equals(transactionReceipt.getStatus())) {
      // Transaction may have failed
      return null;
    }
    return transactionReceipt;
  }

  private EthereumClientConnector getEthereumClientConnector() {
    if (ethereumClientConnector == null) {
      ethereumClientConnector = CommonsUtils.getService(EthereumClientConnector.class);
    }
    return ethereumClientConnector;
  }

  private WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
  }

  private EthereumTransactionDecoder getTransactionDecoderService() {
    if (ethereumTransactionDecoder == null) {
      ethereumTransactionDecoder = CommonsUtils.getService(EthereumTransactionDecoder.class);
    }
    return ethereumTransactionDecoder;
  }

}
