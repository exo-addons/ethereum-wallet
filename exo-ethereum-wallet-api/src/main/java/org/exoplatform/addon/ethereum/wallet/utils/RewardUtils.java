package org.exoplatform.addon.ethereum.wallet.utils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.*;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.ws.frameworks.json.JsonGenerator;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.impl.*;

public class RewardUtils {

  public static final JsonParser    JSON_PARSER              = new JsonParserImpl();

  public static final JsonGenerator JSON_GENERATOR           = new JsonGeneratorImpl();

  public static final String        REWARD_SCOPE_NAME        = "ADDONS_REWARD";

  public static final String        REWARD_CONTEXT_NAME      = "ADDONS_REWARD";

  public static final Context       REWARD_CONTEXT           = Context.GLOBAL.id(REWARD_CONTEXT_NAME);

  public static final Scope         REWARD_SCOPE             =
                                                 Scope.APPLICATION.id(REWARD_SCOPE_NAME);

  public static final String        REWARD_SETTINGS_KEY_NAME = "REWARD_SETTINGS";

  private RewardUtils() {
  }

  public static LocalDateTime timeFromSeconds(long createdDate) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(createdDate), TimeZone.getDefault().toZoneId());
  }

  public static long timeToSeconds(LocalDateTime time) {
    return time.atZone(ZoneOffset.systemDefault()).toEpochSecond();
  }

  public static String decodeString(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLDecoder.decode(content.trim(), "UTF-8");
    } catch (Exception e) {
      return content;
    }
  }

  public static String encodeString(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLEncoder.encode(content.trim(), "UTF-8");
    } catch (Exception e) {
      return content;
    }
  }

  public static final <T> T fromJsonString(String value, Class<T> resultClass) throws JsonException {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    JsonDefaultHandler jsonDefaultHandler = new JsonDefaultHandler();
    JSON_PARSER.parse(new ByteArrayInputStream(value.getBytes()), jsonDefaultHandler);
    return ObjectBuilder.createObject(resultClass, jsonDefaultHandler.getJsonObject());
  }

  public static final String toJsonString(Object object) throws JsonException {
    return JSON_GENERATOR.createJsonObject(object).toString();
  }

  public static final String getCurrentUserId() {
    if (ConversationState.getCurrent() != null && ConversationState.getCurrent().getIdentity() != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null;
  }

  public static final Method getMethod(ExoContainer container, String serviceName, String methodName) {
    Object serviceInstance = getService(container, serviceName);
    if (serviceInstance == null) {
      return null;
    }

    Method methodResult = null;

    int i = 0;
    Method[] declaredMethods = serviceInstance.getClass().getDeclaredMethods();
    while (methodResult == null && i < declaredMethods.length) {
      Method method = declaredMethods[i++];
      if (method.getName().equals(methodName)) {
        methodResult = method;
      }
    }
    return methodResult;
  }

  public static final Object getService(ExoContainer container, String serviceName) {
    Object serviceInstance = null;
    try {
      serviceInstance = container.getComponentInstanceOfType(Class.forName(serviceName));
    } catch (ClassNotFoundException e) {
      return null;
    }
    return serviceInstance;
  }

}
