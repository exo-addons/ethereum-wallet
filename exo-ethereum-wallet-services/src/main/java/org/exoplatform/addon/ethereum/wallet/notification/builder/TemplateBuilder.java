package org.exoplatform.addon.ethereum.wallet.notification.builder;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.io.Writer;
import java.util.Calendar;
import java.util.Locale;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.config.PluginConfig;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.api.notification.template.Element;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.webui.utils.TimeConvertUtils;

public class TemplateBuilder extends AbstractTemplateBuilder {

  private TemplateProvider templateProvider;

  public TemplateBuilder(TemplateProvider templateProvider) {
    this.templateProvider = templateProvider;
  }

  @Override
  protected MessageInfo makeMessage(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    String pluginId = notification.getKey().getId();

    String language = getLanguage(notification);
    TemplateContext templateContext =
                                    TemplateContext.newChannelInstance(this.templateProvider.getChannelKey(), pluginId, language);

    String amount = notification.getValueOwnerParameter(AMOUNT);
    String avatar = notification.getValueOwnerParameter(AVATAR);
    String receiver = notification.getValueOwnerParameter(RECEIVER);
    String sender = notification.getValueOwnerParameter(SENDER);
    String profileURL = notification.getValueOwnerParameter(PROFILE_URL);
    String contract = notification.getValueOwnerParameter(CONTRACT);

    templateContext.put("AMOUNT", amount);
    templateContext.put("SENDER", sender);
    templateContext.put("RECEIVER", receiver);
    templateContext.put("PROFILE_URL", profileURL);
    templateContext.put("AVATAR", avatar != null ? avatar : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
    if (contract != null) {
      templateContext.put("CONTRACT", contract);
    }
    templateContext.put("NOTIFICATION_ID", notification.getId());
    templateContext.put("LAST_UPDATED_TIME", getLastModifiedDate(notification, language));
    templateContext.put("READ",
                        Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read"
                                                                                                                              : "unread");

    String body = TemplateUtils.processGroovy(templateContext);
    // binding the exception throws by processing template
    ctx.setException(templateContext.getException());
    MessageInfo messageInfo = new MessageInfo();
    addMessageSubject(messageInfo, templateContext);
    return messageInfo.body(body).end();
  }

  @Override
  protected boolean makeDigest(NotificationContext ctx, Writer writer) {
    return false;
  }

  private void addMessageSubject(MessageInfo messageInfo, TemplateContext templateContext) {
    PluginConfig templateConfig = getPluginConfig(templateContext.getPluginId());
    Element subjectElement = NotificationUtils.getSubject(templateConfig,
                                                          templateContext.getPluginId(),
                                                          templateContext.getLanguage());
    if (subjectElement != null && subjectElement.getTemplate() != null) {
      messageInfo.subject(subjectElement.getTemplate());
    }
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

  private static PluginConfig getPluginConfig(String pluginId) {
    PluginConfig pluginConfig = NotificationContextImpl.cloneInstance().getPluginSettingService().getPluginConfig(pluginId);

    if (pluginConfig == null) {
      throw new IllegalStateException("PluginConfig is NULL with plugId = " + pluginId);
    }

    return pluginConfig;
  }

};
