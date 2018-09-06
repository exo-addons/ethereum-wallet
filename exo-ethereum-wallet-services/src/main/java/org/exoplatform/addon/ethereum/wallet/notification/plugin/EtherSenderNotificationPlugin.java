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

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import org.exoplatform.addon.ethereum.wallet.model.AccountDetail;
import org.exoplatform.addon.ethereum.wallet.model.TransactionStatus;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.service.LinkProvider;

public class EtherSenderNotificationPlugin extends BaseNotificationPlugin {

  public EtherSenderNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return TransactionStatus.SENDER.getNotificationId();
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    AccountDetail senderAccountDetail = ctx.value(SENDER_ACCOUNT_DETAIL_PARAMETER);
    AccountDetail receiverAccountDetail = ctx.value(RECEIVER_ACCOUNT_DETAIL_PARAMETER);
    double amount = ctx.value(AMOUNT_PARAMETER);

    String profileLink = receiverAccountDetail.getId() == null ? receiverAccountDetail.getName()
                                                               : LinkProvider.getProfileLink(receiverAccountDetail.getId());

    return NotificationInfo.instance()
                           .to(getNotificationReceiversUsers(receiverAccountDetail, senderAccountDetail.getId()))
                           .with(AMOUNT, String.valueOf(amount))
                           .with(AVATAR, CommonsUtils.getCurrentDomain() + receiverAccountDetail.getAvatar())
                           .with(PROFILE_URL, profileLink)
                           .with(SENDER, senderAccountDetail.getName())
                           .with(RECEIVER, receiverAccountDetail.getName())
                           .key(getKey())
                           .end();
  }
}
