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
import org.exoplatform.addon.ethereum.wallet.service.EthereumClientConnector;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.*;

/**
 * A listener to listen to transactions made on newly configured global settings
 */
@Asynchronous
public class GlobalSettingsModificationListener extends Listener<Object, GlobalSettings> {
  private EthereumClientConnector ethereumClientConnector;

  @Override
  public void onEvent(Event<Object, GlobalSettings> event) throws Exception {
    GlobalSettings globalSettings = event.getData();
    if (globalSettings != null) {
      getEthereumClientConnector().changeSettings(globalSettings);
    }
  }

  public EthereumClientConnector getEthereumClientConnector() {
    if (ethereumClientConnector == null) {
      ethereumClientConnector = CommonsUtils.getService(EthereumClientConnector.class);
    }
    return ethereumClientConnector;
  }

}
