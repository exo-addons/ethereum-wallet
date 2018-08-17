package org.exoplatform.addon.ethereum.wallet.rest;

import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.security.ConversationState;

public class Utils {

  public static final String  DEFAULT_NETWORK_ID                 = "defaultNetworkId";

  public static final String  DEFAULT_NETWORK_URL                = "defaultNetworkURL";

  public static final String  DEFAULT_ACCESS_PERMISSION          = "defaultAccessPermission";

  public static final String  DEFAULT_GAS                        = "defaultGas";

  public static final String  DEFAULT_BLOCKS_TO_RETRIEVE         = "defaultBlocksToRetrieve";

  public static final String  DEFAULT_CONTRACTS_ADDRESSES        = "defaultContractAddresses";

  public static final String  DEFAULT_CONTRACTS_ADDRESSES_STORED = "DEFAULT_CONTRACTS_ADDRESSES_STORED";

  public static final String  SCOPE_NAME                         = "ADDONS_ETHEREUM_WALLET";

  public static final String  GLOBAL_SETTINGS_KEY_NAME           = "GLOBAL_SETTINGS";

  public static final String  ADDRESS_KEY_NAME                   = "ADDONS_ETHEREUM_WALLET_ADDRESS";

  public static final String  SETTINGS_KEY_NAME                  = "ADDONS_ETHEREUM_WALLET_SETTINGS";

  public static final Context WALLET_CONTEXT                     = Context.GLOBAL;

  public static final Scope   WALLET_SCOPE                       = Scope.APPLICATION.id(SCOPE_NAME);

  public static final String  WALLET_DEFAULT_CONTRACTS_NAME      = "WALLET_DEFAULT_CONTRACTS";

  public static final String getCurrentUserId() {
    if (ConversationState.getCurrent() != null && ConversationState.getCurrent().getIdentity() != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null;
  }
}
