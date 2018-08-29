package org.exoplatform.addon.ethereum.wallet.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

public enum TransactionStatus {
  RECEIVER(TRANSACTION_RECEIVER_NOTIFICATION_ID), SENDER(TRANSACTION_SENDER_NOTIFICATION_ID), CONTRACT_RECEIVER(
      TRANSACTION_CONTRACT_RECEIVER_NOTIFICATION_ID), CONTRACT_SENDER(TRANSACTION_CONTRACT_SENDER_NOTIFICATION_ID);

  private String notificationId;

  private TransactionStatus(String notificationId) {
    this.notificationId = notificationId;
  }

  public String getNotificationId() {
    return notificationId;
  }
}
