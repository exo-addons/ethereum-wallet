package org.exoplatform.addon.ethereum.wallet.listener;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.USER_ACCOUNT_TYPE;

import java.util.Map;
import java.util.Set;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * This listener will be triggered when a new address is associated to a user or
 * a space. Thus an initial funds request should be sent to funds holder when a
 * new address is added.
 */
public class InitialFundsRequestListener extends Listener<Object, AccountDetail> {

  private static final Log      LOG = ExoLogger.getLogger(InitialFundsRequestListener.class);

  private EthereumWalletService ethereumWalletService;

  public InitialFundsRequestListener(EthereumWalletService ethereumWalletService) {
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
    if (initialFunds == null || initialFunds.isEmpty() || settings.getFundsHolder() == null || settings.getFundsHolder().isEmpty()
        || accountDetail.getId() == null || settings.getFundsHolder().equals(accountDetail.getId())) {
      return;
    }

    Set<String> addresses = initialFunds.keySet();
    for (String address : addresses) {
      Double amount = initialFunds.get(address);
      if (amount == null || amount == 0) {
        LOG.info("Fund request amount is 0, thus no notification will be sent.", address);
        continue;
      }

      address = address.toLowerCase();
      FundsRequest request = new FundsRequest();
      if (!"ether".equalsIgnoreCase(address)) {
        // If contract adress is not a default one anymore, skip
        if (!settings.getDefaultContractsToDisplay().contains(address)) {
          LOG.warn("Can't find contract with address {}. No fund request notification will be sent.", address);
          continue;
        }
        request.setContract(address);
      }
      request.setAmount(amount);
      request.setAddress(accountDetail.getAddress());
      request.setReceipient(settings.getFundsHolder());
      request.setReceipientType(settings.getFundsHolderType());
      request.setMessage(settings.getInitialFundsRequestMessage());

      this.ethereumWalletService.requestFunds(request);
    }
  }
}
