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
package org.exoplatform.addon.ethereum.wallet.notification.provider;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.io.Writer;
import java.util.Calendar;
import java.util.Locale;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.*;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.webui.utils.TimeConvertUtils;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = TRANSACTION_SENDER_NOTIFICATION_ID, template = "jar:/templates/notification/web/WalletSenderPlugin.gtmpl"),
    @TemplateConfig(pluginId = TRANSACTION_RECEIVER_NOTIFICATION_ID, template = "jar:/templates/notification/web/WalletReceiverPlugin.gtmpl"),
    @TemplateConfig(pluginId = TRANSACTION_CONTRACT_SENDER_NOTIFICATION_ID, template = "jar:/templates/notification/web/WalletContractSenderPlugin.gtmpl"),
    @TemplateConfig(pluginId = TRANSACTION_CONTRACT_RECEIVER_NOTIFICATION_ID, template = "jar:/templates/notification/web/WalletContractReceiverPlugin.gtmpl") })
public class MobilePushTemplateProvider extends TemplateProvider {

  public MobilePushTemplateProvider(InitParams initParams) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(TRANSACTION_SENDER_NOTIFICATION_ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TRANSACTION_RECEIVER_NOTIFICATION_ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TRANSACTION_CONTRACT_SENDER_NOTIFICATION_ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TRANSACTION_CONTRACT_RECEIVER_NOTIFICATION_ID), new TemplateBuilder());
  }

  private class TemplateBuilder extends AbstractTemplateBuilder {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();
      String pluginId = notification.getKey().getId();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), pluginId, language);

      String amount = notification.getValueOwnerParameter("amount");
      String avatar = notification.getValueOwnerParameter("avatar");
      String fullName = notification.getValueOwnerParameter("user");

      templateContext.put("AMOUNT", amount);
      templateContext.put("USER", fullName);
      templateContext.put("AVATAR", avatar != null ? avatar : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", getLastModifiedDate(notification, language));
      templateContext.put("READ",
                          Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read"
                                                                                                                                : "unread");

      String body = TemplateUtils.processGroovy(templateContext);
      // binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
    }

    private String getLastModifiedDate(NotificationInfo notification, String language) {
      Calendar lastModified = Calendar.getInstance();
      lastModified.setTimeInMillis(notification.getLastModifiedDate());
      String date = TimeConvertUtils.convertXTimeAgoByTimeServer(lastModified.getTime(),
                                                                 "EE, dd yyyy",
                                                                 new Locale(language),
                                                                 TimeConvertUtils.YEAR);
      return date;
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }

  };

}
