package org.exoplatform.addon.ethereum.wallet.listener;

import org.exoplatform.addon.ethereum.wallet.model.Wallet;
import org.exoplatform.addon.ethereum.wallet.service.WalletTokenTransactionService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class ModifiedAdminWalletListener extends Listener<Wallet, Wallet> {

  private WalletTokenTransactionService tokenTransactionService;

  @Override
  public void onEvent(Event<Wallet, Wallet> event) throws Exception {
    getTokenTransactionService().reinit();
  }

  private WalletTokenTransactionService getTokenTransactionService() {
    if (tokenTransactionService == null) {
      tokenTransactionService = CommonsUtils.getService(WalletTokenTransactionService.class);
    }
    return tokenTransactionService;
  }

}
