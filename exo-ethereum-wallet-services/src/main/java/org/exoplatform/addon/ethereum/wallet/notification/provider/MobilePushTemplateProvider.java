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

import org.exoplatform.addon.ethereum.wallet.notification.builder.RequestFundsTemplateBuilder;
import org.exoplatform.addon.ethereum.wallet.notification.builder.TemplateBuilder;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.container.xml.InitParams;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = TRANSACTION_SENDER_NOTIFICATION_ID, template = "war:/conf/ethereum-wallet/templates/notification/push/SenderPlugin.gtmpl"),
    @TemplateConfig(pluginId = TRANSACTION_RECEIVER_NOTIFICATION_ID, template = "war:/conf/ethereum-wallet/templates/notification/push/ReceiverPlugin.gtmpl"),
    @TemplateConfig(pluginId = TRANSACTION_CONTRACT_SENDER_NOTIFICATION_ID, template = "war:/conf/ethereum-wallet/templates/notification/push/ContractSenderPlugin.gtmpl"),
    @TemplateConfig(pluginId = TRANSACTION_CONTRACT_RECEIVER_NOTIFICATION_ID, template = "war:/conf/ethereum-wallet/templates/notification/push/ContractReceiverPlugin.gtmpl"),
    @TemplateConfig(pluginId = FUNDS_REQUEST_NOTIFICATION_ID, template = "war:/conf/ethereum-wallet/templates/notification/push/RequestFundsPlugin.gtmpl") })
public class MobilePushTemplateProvider extends TemplateProvider {

  public MobilePushTemplateProvider(InitParams initParams) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(TRANSACTION_SENDER_NOTIFICATION_ID), new TemplateBuilder(this));
    this.templateBuilders.put(PluginKey.key(TRANSACTION_RECEIVER_NOTIFICATION_ID), new TemplateBuilder(this));
    this.templateBuilders.put(PluginKey.key(TRANSACTION_CONTRACT_SENDER_NOTIFICATION_ID), new TemplateBuilder(this));
    this.templateBuilders.put(PluginKey.key(TRANSACTION_CONTRACT_RECEIVER_NOTIFICATION_ID), new TemplateBuilder(this));
    this.templateBuilders.put(PluginKey.key(FUNDS_REQUEST_NOTIFICATION_ID), new RequestFundsTemplateBuilder(this));
  }

}
