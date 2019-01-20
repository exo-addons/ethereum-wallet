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

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;

/**
 * A listener to asynchronously save last watched block number
 */
@Asynchronous
public class EthereumLastWatchedBlockListener extends Listener<Long, Object> {

  private EthereumWalletService ethereumWalletService;

  private ExoContainer          container;

  private long                  lastSavedBlockNumber = 0;

  private long                  networkId            = 0;

  public EthereumLastWatchedBlockListener(ExoContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<Long, Object> event) throws Exception {
    Long blockNumber = event.getSource();
    if (blockNumber == null) {
      return;
    }
    RequestLifeCycle.begin(this.container);
    try {
      GlobalSettings globalSettings = getEthereumWalletService().getSettings();
      long defaultNetworkId = globalSettings.getDefaultNetworkId();
      if (defaultNetworkId != this.networkId || blockNumber > this.lastSavedBlockNumber) {
        this.lastSavedBlockNumber = blockNumber;
        this.networkId = defaultNetworkId;
        getEthereumWalletService().saveLastWatchedBlockNumber(defaultNetworkId, blockNumber);
      }
    } finally {
      RequestLifeCycle.end();
    }
  }

  public EthereumWalletService getEthereumWalletService() {
    if (ethereumWalletService == null) {
      ethereumWalletService = CommonsUtils.getService(EthereumWalletService.class);
    }
    return ethereumWalletService;
  }
}
