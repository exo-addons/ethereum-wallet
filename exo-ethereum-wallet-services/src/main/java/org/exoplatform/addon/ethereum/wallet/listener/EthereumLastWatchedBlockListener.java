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

import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletStorage;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;

/**
 * A listener to asynchronously save last watched block number
 */
@Asynchronous
public class EthereumLastWatchedBlockListener extends Listener<Transaction, TransactionReceipt> {

  private EthereumWalletStorage ethereumWalletStorage;

  private ExoContainer          container;

  private long                  lastSavedBlockNumber = 0;

  private long                  networkId            = 0;

  public EthereumLastWatchedBlockListener(ExoContainer container, EthereumWalletStorage ethereumWalletStorage) {
    this.ethereumWalletStorage = ethereumWalletStorage;
    this.container = container;
  }

  @Override
  public void onEvent(Event<Transaction, TransactionReceipt> event) throws Exception {
    Transaction transaction = event.getSource();
    if (transaction == null || transaction.getChainId() == null || transaction.getBlockNumber() == null) {
      return;
    }
    RequestLifeCycle.begin(this.container);
    try {
      long blockNumber = transaction.getBlockNumber().longValue();
      long networkId = 0;
      if (transaction.getChainId() == null) {
        GlobalSettings globalSettings = ethereumWalletStorage.getSettings(null, null);
        networkId = globalSettings.getDefaultNetworkId();
      } else {
        networkId = transaction.getChainId().longValue();
      }
      if (networkId != this.networkId || blockNumber > this.lastSavedBlockNumber) {
        this.ethereumWalletStorage.saveLastWatchedBlockNumber(networkId, blockNumber);
        this.lastSavedBlockNumber = blockNumber;
      }
    } finally {
      RequestLifeCycle.end();
    }
  }
}
