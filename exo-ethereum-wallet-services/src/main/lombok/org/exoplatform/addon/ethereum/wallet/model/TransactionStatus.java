package org.exoplatform.addon.ethereum.wallet.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.WALLET_RECEIVER_NOTIFICATION_ID;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.WALLET_SENDER_NOTIFICATION_ID;

public enum TransactionStatus {
  RECEIVER(WALLET_RECEIVER_NOTIFICATION_ID),
  SENDER(WALLET_SENDER_NOTIFICATION_ID);

  private String notificationId;

  private TransactionStatus(String notificationId) {
    this.notificationId = notificationId;
  }

  public String getNotificationId() {
    return notificationId;
  }
}
