package org.exoplatform.addon.ethereum.wallet.service;

import org.exoplatform.addon.ethereum.wallet.model.AccountDetail;
import org.exoplatform.addon.ethereum.wallet.model.TransactionStatus;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.services.security.ConversationState;

/**
 * Utils class to provide common tools
 */
public class Utils {
  public static final String                       GLOAL_SETTINGS_CHANGED_EVENT                  =
                                                                                "exo.addon.wallet.settings.changed";

  public static final String                       NEW_TRANSACTION_EVENT                         =
                                                                         "exo.addon.wallet.transaction.loaded";

  public static final String                       TRANSACTION_SENDER_NOTIFICATION_ID            =
                                                                                      "exo.addon.wallet.transaction.notification.sender";

  public static final String                       TRANSACTION_RECEIVER_NOTIFICATION_ID          =
                                                                                        "exo.addon.wallet.transaction.notification.receiver";

  public static final String                       TRANSACTION_CONTRACT_SENDER_NOTIFICATION_ID   =
                                                                                               "exo.addon.wallet.transaction.notification.contract.sender";

  public static final String                       TRANSACTION_CONTRACT_RECEIVER_NOTIFICATION_ID =
                                                                                                 "exo.addon.wallet.transaction.notification.contract.receiver";

  public static ArgumentLiteral<AccountDetail>     ACCOUNT_DETAIL_PARAMETER                      =
                                                                            new ArgumentLiteral<AccountDetail>(AccountDetail.class,
                                                                                                               "accountDetail");

  public static ArgumentLiteral<TransactionStatus> TRANSACTION_STATUS_PARAMETER                  =
                                                                                new ArgumentLiteral<TransactionStatus>(TransactionStatus.class,
                                                                                                                       "transactionStatus");

  public static ArgumentLiteral<Integer>           AMOUNT_PARAMETER                              =
                                                                    new ArgumentLiteral<Integer>(Integer.class, "amount");

  public static final String getCurrentUserId() {
    if (ConversationState.getCurrent() != null && ConversationState.getCurrent().getIdentity() != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null;
  }

  public static void setTimeout(Runnable runnable, int delay) {
    new Thread(() -> {
      try {
        Thread.sleep(delay);
        runnable.run();
      } catch (Exception e) {
        System.err.println(e);
      }
    }).start();
  }
}
