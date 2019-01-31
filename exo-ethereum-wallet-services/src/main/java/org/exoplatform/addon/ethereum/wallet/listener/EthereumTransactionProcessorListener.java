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

import java.util.concurrent.ExecutionException;

import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.addon.ethereum.wallet.service.EthereumClientConnector;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletTransactionService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;

/**
 * A listener to process newly detected transactions coming from configured
 * network
 */
@Asynchronous
public class EthereumTransactionProcessorListener extends Listener<Transaction, TransactionReceipt> {

  private EthereumWalletTransactionService transactionService;

  private EthereumClientConnector          ethereumClientConnector;

  private ExoContainer                     container;

  public EthereumTransactionProcessorListener(ExoContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<Transaction, TransactionReceipt> event) throws Exception {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      Transaction transaction = event.getSource();
      if (transaction == null) {
        return;
      }

      TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(transaction.getHash(), true);
      if (transactionDetail == null) {
        return;
      }
      TransactionReceipt transactionReceipt = event.getData();
      if (transactionReceipt == null) {
        try {
          transactionReceipt = getTransactionReceipt(transaction);
        } catch (Exception e) {
          // Attempt another time to get receipt
          transactionReceipt = getTransactionReceipt(transaction);
        }
      }
      transactionDetail.setPending(false);
      transactionDetail.setSucceeded(transactionReceipt != null && transactionReceipt.isStatusOK());

      getTransactionService().saveTransactionDetail(transactionDetail, null, true);
    } finally {
      RequestLifeCycle.end();
    }
  }

  private TransactionReceipt getTransactionReceipt(Transaction transaction) throws InterruptedException, ExecutionException {
    TransactionReceipt transactionReceipt = getEthereumClientConnector().getTransactionReceipt(transaction.getHash());
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
