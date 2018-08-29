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
                                                                                      "EtherSenderNotificationPlugin";

  public static final String                       TRANSACTION_RECEIVER_NOTIFICATION_ID          =
                                                                                        "EtherReceiverNotificationPlugin";

  public static final String                       TRANSACTION_CONTRACT_SENDER_NOTIFICATION_ID   =
                                                                                               "ContractSenderNotificationPlugin";

  public static final String                       TRANSACTION_CONTRACT_RECEIVER_NOTIFICATION_ID =
                                                                                                 "ContractReceiverNotificationPlugin";

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
