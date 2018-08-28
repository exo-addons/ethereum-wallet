package org.exoplatform.addon.ethereum.wallet.listener;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.service.EthereumClientConnector;
import org.exoplatform.services.listener.*;

/**
 * A listener to listen to transactions made on newly configured network
 */
@Asynchronous
public class GlobalSettingsModificationListener extends Listener<Object, GlobalSettings> {
  private EthereumClientConnector ethereumClientConnector;

  public GlobalSettingsModificationListener(EthereumClientConnector ethereumClientConnector) {
    this.ethereumClientConnector = ethereumClientConnector;
  }

  @Override
  public void onEvent(Event<Object, GlobalSettings> event) throws Exception {
    GlobalSettings globalSettings = event.getData();
    if (globalSettings != null && globalSettings.getWebsocketProviderURL() != null
        && !StringUtils.equals(globalSettings.getWebsocketProviderURL(), ethereumClientConnector.getWebsocketProviderURL())) {
      ethereumClientConnector.changeWebsocketProviderURL(globalSettings.getWebsocketProviderURL());
    }
  }
}
