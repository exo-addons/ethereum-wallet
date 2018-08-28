package org.exoplatform.addon.ethereum.wallet.rest;

import org.exoplatform.services.security.ConversationState;

/**
 * Utils class to provide common tools
 */
public class Utils {
  public static final String GLOAL_SETTINGS_CHANGED_EVENT = "exo.addon.wallet.settings.changed";

  public static final String NEW_TRANSACTION_EVENT        = "exo.addon.wallet.transaction.loaded";

  public static final String getCurrentUserId() {
    if (ConversationState.getCurrent() != null && ConversationState.getCurrent().getIdentity() != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null;
  }
}
