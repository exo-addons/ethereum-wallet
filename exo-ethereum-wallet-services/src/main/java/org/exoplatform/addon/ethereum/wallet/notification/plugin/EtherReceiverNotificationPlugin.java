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
package org.exoplatform.addon.ethereum.wallet.notification.plugin;

import static org.exoplatform.addon.ethereum.wallet.service.Utils.ACCOUNT_DETAIL_PARAMETER;
import static org.exoplatform.addon.ethereum.wallet.service.Utils.AMOUNT_PARAMETER;

import java.util.Collections;

import org.exoplatform.addon.ethereum.wallet.model.AccountDetail;
import org.exoplatform.addon.ethereum.wallet.model.TransactionStatus;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;

public class EtherReceiverNotificationPlugin extends BaseNotificationPlugin {

  public EtherReceiverNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return TransactionStatus.RECEIVER.getNotificationId();
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    AccountDetail accountDetail = ctx.value(ACCOUNT_DETAIL_PARAMETER);
    int amount = ctx.value(AMOUNT_PARAMETER);

    return NotificationInfo.instance()
                           .to(Collections.singletonList(accountDetail.getId()))
                           .with("amount", String.valueOf(amount))
                           .with("avatar", accountDetail.getAvatar())
                           .with("user", accountDetail.getName())
                           .key(getKey())
                           .end();
  }
}
