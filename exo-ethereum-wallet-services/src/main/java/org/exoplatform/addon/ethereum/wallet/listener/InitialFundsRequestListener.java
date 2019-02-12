package org.exoplatform.addon.ethereum.wallet.listener;

import static org.exoplatform.addon.ethereum.wallet.utils.Utils.getCurrentUserId;

import java.util.Map;
import java.util.Set;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * This listener will be triggered when a new address is associated to a user or
 * a space. Thus an initial funds request should be sent to funds holder when an
 * address is added or modified.
 */
@Asynchronous
public class InitialFundsRequestListener extends Listener<Wallet, Wallet> {

  private static final Log      LOG = ExoLogger.getLogger(InitialFundsRequestListener.class);

  private EthereumWalletService ethereumWalletService;

  @Override
  public void onEvent(Event<Wallet, Wallet> event) throws Exception {
    Wallet wallet = event.getData();

    GlobalSettings settings = getEthereumWalletService().getSettings();
    Map<String, Double> initialFunds = settings.getInitialFunds();
    if (initialFunds == null || initialFunds.isEmpty() || settings.getFundsHolder() == null || settings.getFundsHolder().isEmpty()
        || wallet.getId() == null || settings.getFundsHolder().equals(wallet.getId())) {
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
      request.setAddress(wallet.getAddress());
      request.setReceipient(settings.getFundsHolder());
      request.setReceipientType(settings.getFundsHolderType());
      request.setMessage("A new wallet has been created");

      try {
        getEthereumWalletService().requestFunds(request);
      } catch (Exception e) {
        LOG.error("Unknown error occurred while user '" + getCurrentUserId() + "' requesting funds for wallet of type '"
            + wallet.getType() + "' with id '" + wallet.getId() + "'", e);
        throw e;
      }
    }
  }

  public EthereumWalletService getEthereumWalletService() {
    if (ethereumWalletService == null) {
      ethereumWalletService = CommonsUtils.getService(EthereumWalletService.class);
    }
    return ethereumWalletService;
  }

}
