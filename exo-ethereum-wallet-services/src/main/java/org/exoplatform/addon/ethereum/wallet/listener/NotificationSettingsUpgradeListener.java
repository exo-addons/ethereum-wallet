package org.exoplatform.addon.ethereum.wallet.listener;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.storage.WebNotificationStorage;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * This listener will asynchronously change old settings from old IDs.
 * 
 * TODO temporary use this to migrate notification settings
 */
@Asynchronous
public class NotificationSettingsUpgradeListener extends Listener<Object, String> {

  private static final Log       LOG = ExoLogger.getLogger(NotificationSettingsUpgradeListener.class);

  private PortalContainer        portalContainer;

  private WebNotificationStorage webNotificationStorage;

  public NotificationSettingsUpgradeListener(PortalContainer portalContainer, WebNotificationStorage webNotificationStorage) {
    this.webNotificationStorage = webNotificationStorage;
    this.portalContainer = portalContainer;
  }

  @Override
  public void onEvent(Event<Object, String> event) throws Exception {
    String notificationId = event.getData();
    if (StringUtils.isBlank(notificationId)) {
      return;
    }

    RequestLifeCycle.begin(portalContainer);
    try {
      NotificationInfo notificationInfo = webNotificationStorage.get(notificationId);
      if (notificationInfo != null) {
        String pluginId = notificationInfo.getKey().getId();
        if (!isToUpgrade(pluginId)) {
          return;
        }
        String newPluginId = getNewId(pluginId);

        LOG.info("Upgrading notification plugin from {} to {} for notification with id {}",
                 pluginId,
                 newPluginId,
                 notificationInfo.getId());
        notificationInfo.key(PluginKey.key(newPluginId));
        webNotificationStorage.update(notificationInfo, false);
      }
    } catch (Exception e) {
      LOG.error("Error upgrading notification plugin", e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  private boolean isToUpgrade(String pluginId) {
    return DEPRECATED_RECEIVER_NOTIFICATION_ID.equals(pluginId) || DEPRECATED_SENDER_NOTIFICATION_ID.equals(pluginId);
  }

  private String getNewId(String pluginId) {
    return DEPRECATED_RECEIVER_NOTIFICATION_ID.equals(pluginId) ? WALLET_RECEIVER_NOTIFICATION_ID : WALLET_SENDER_NOTIFICATION_ID;
  }
}
