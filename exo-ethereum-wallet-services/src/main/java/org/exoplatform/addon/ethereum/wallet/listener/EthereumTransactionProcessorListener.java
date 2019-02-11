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
import org.exoplatform.addon.ethereum.wallet.service.EthereumClientConnector;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletTransactionService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A listener to process newly detected transactions coming from configured
 * network
 */
@Asynchronous
public class EthereumTransactionProcessorListener extends Listener<Object, TransactionReceipt> {

  private static final Log                 LOG = ExoLogger.getLogger(EthereumTransactionProcessorListener.class);

  private EthereumWalletTransactionService transactionService;

  private EthereumClientConnector          ethereumClientConnector;

  private ExoContainer                     container;

  public EthereumTransactionProcessorListener(ExoContainer container) {
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
      Long blockTimestamp = null;
      if (source instanceof MinedTransactionDetail) {
        MinedTransactionDetail transaction = (MinedTransactionDetail) source;
        transactionHash = transaction.getHash();
        blockTimestamp = transaction.getBlockTimestamp();
      } else if (source instanceof Transaction) {
        Transaction transaction = (Transaction) source;
        transactionHash = transaction.getHash();
        Block block = getEthereumClientConnector().getBlock(transaction.getBlockHash());
        blockTimestamp = block.getTimestamp().longValue();
      } else {
        transactionHash = (String) source;
      }

      if (StringUtils.isBlank(transactionHash)) {
        LOG.warn("Transaction hash is empty");
      }

      TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(transactionHash, true);
      if (transactionDetail == null) {
        return;
      }

      TransactionReceipt transactionReceipt = event.getData();
      if (transactionReceipt == null) {
        transactionReceipt = getTransactionReceipt(transactionHash);
      }
      transactionDetail.setPending(false);
      transactionDetail.setSucceeded(transactionReceipt != null && transactionReceipt.isStatusOK());
      if (blockTimestamp != null && transactionDetail.getTimestamp() == 0) {
        transactionDetail.setTimestamp(blockTimestamp);
      }

      getTransactionService().saveTransactionDetail(transactionDetail, null, true);
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

  private EthereumWalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(EthereumWalletTransactionService.class);
    }
    return transactionService;
  }

}
