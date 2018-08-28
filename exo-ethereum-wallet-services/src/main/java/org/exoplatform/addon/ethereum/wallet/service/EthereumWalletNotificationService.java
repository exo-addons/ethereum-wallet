package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.Utils.*;

import org.exoplatform.addon.ethereum.wallet.model.AccountDetail;
import org.exoplatform.addon.ethereum.wallet.model.TransactionStatus;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;

public class EthereumWalletNotificationService {

  public void sendNotification(AccountDetail accountDetail, TransactionStatus transactionStatus, int amount) {
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(ACCOUNT_DETAIL_PARAMETER, accountDetail);
    ctx.append(AMOUNT_PARAMETER, amount);

    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(transactionStatus.getNotificationId()))).execute(ctx);
  }

}
