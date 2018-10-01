package org.exoplatform.addon.ethereum.wallet.listener;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.USER_ACCOUNT_TYPE;

import java.util.Map;
import java.util.Set;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

/**
 * This listener will be triggered when a new address is associated to a user or
 * a space. Thus an initial funds request should be sent to funds holder when a
 * new address is added.
 */
public class NewAddressAssociationListener extends Listener<Object, AccountDetail> {

  private EthereumWalletService ethereumWalletService;

  public NewAddressAssociationListener(EthereumWalletService ethereumWalletService) {
    this.ethereumWalletService = ethereumWalletService;
  }

  @Override
  public void onEvent(Event<Object, AccountDetail> event) throws Exception {
    AccountDetail accountDetail = event.getData();
    if (accountDetail == null || !USER_ACCOUNT_TYPE.equals(accountDetail.getType())) {
      return;
    }

    GlobalSettings settings = ethereumWalletService.getSettings();
    Map<String, Double> initialFunds = settings.getInitialFunds();
    if (initialFunds == null || initialFunds.isEmpty()) {
      return;
    }

    Set<String> addresses = initialFunds.keySet();
    for (String address : addresses) {
      Double amount = initialFunds.get(address);
      if (amount == null || amount == 0) {
        continue;
      }

      FundsRequest request = new FundsRequest();
      if (!"ether".equals(address)) {
        request.setContract(address);
      }
      request.setAmount(amount);
      request.setAddress(accountDetail.getAddress());
      request.setReceipient(settings.getFundsHolder());
      request.setReceipientType(settings.getFundsHolderType());
      request.setMessage(settings.getInitialfundsRequestMessage());

      this.ethereumWalletService.requestFunds(request);

    }

  }

}