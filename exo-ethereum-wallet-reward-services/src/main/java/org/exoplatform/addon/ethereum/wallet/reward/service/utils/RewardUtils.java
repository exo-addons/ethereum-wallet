package org.exoplatform.addon.ethereum.wallet.reward.service.utils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.reward.entity.RewardTeamEntity;
import org.exoplatform.addon.ethereum.wallet.reward.entity.RewardTeamMemberEntity;
import org.exoplatform.addon.ethereum.wallet.reward.model.RewardTeam;
import org.exoplatform.addon.ethereum.wallet.reward.model.RewardTeamMember;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
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

  public static RewardTeamEntity fromDTO(RewardTeam rewardTeam) {
    if (rewardTeam == null) {
      return null;
    }
    RewardTeamEntity teamEntity = new RewardTeamEntity();
    teamEntity.setId(rewardTeam.getId() == null || rewardTeam.getId() == 0 ? null : rewardTeam.getId());
    teamEntity.setName(rewardTeam.getName());
    teamEntity.setDescription(rewardTeam.getDescription());
    teamEntity.setBudget(rewardTeam.getBudget());
    teamEntity.setRewardType(rewardTeam.getRewardType());
    teamEntity.setRewardPerMember(rewardTeam.getRewardPerMember());
    teamEntity.setDisabled(rewardTeam.isDisabled());
    if (rewardTeam.getManager() != null && rewardTeam.getManager().getIdentityId() != 0) {
      teamEntity.setManager(rewardTeam.getManager().getIdentityId());
    }
    if (rewardTeam.getSpaceId() != null && rewardTeam.getSpaceId() != 0) {
      teamEntity.setSpaceId(rewardTeam.getSpaceId());
    }
    if (rewardTeam.getMembers() != null && !rewardTeam.getMembers().isEmpty()) {
      teamEntity.setMembers(rewardTeam.getMembers()
                                      .stream()
                                      .map(rewardTeamMember -> getRewardTeamMemberEntity(teamEntity,
                                                                                         rewardTeamMember))
                                      .collect(Collectors.toSet()));
    }
    return teamEntity;
  }

  public static RewardTeam toDTO(RewardTeamEntity teamEntity) {
    if (teamEntity == null) {
      return null;
    }
    RewardTeam rewardTeam = new RewardTeam();
    rewardTeam.setId(teamEntity.getId());
    rewardTeam.setName(teamEntity.getName());
    rewardTeam.setDescription(teamEntity.getDescription());
    rewardTeam.setBudget(teamEntity.getBudget());
    rewardTeam.setManager(getRewardTeamMember(teamEntity.getManager()));
    rewardTeam.setRewardType(teamEntity.getRewardType());
    rewardTeam.setRewardPerMember(teamEntity.getRewardPerMember());
    rewardTeam.setDisabled(teamEntity.getDisabled());
    if (teamEntity.getSpaceId() != null && teamEntity.getSpaceId() != 0) {
      SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
      Space space = spaceService.getSpaceById(String.valueOf(teamEntity.getSpaceId()));
      if (space != null) {
        rewardTeam.setSpaceId(teamEntity.getSpaceId());
        rewardTeam.setSpacePrettyName(space.getPrettyName());
      }
    }
    if (teamEntity.getMembers() != null && !teamEntity.getMembers().isEmpty()) {
      List<RewardTeamMember> list = teamEntity.getMembers()
                                              .stream()
                                              .map(teamMemberEntity -> getRewardTeamMember(teamMemberEntity))
                                              .collect(Collectors.toList());
      rewardTeam.setMembers(new ArrayList<>(list));
    }
    return rewardTeam;
  }

  private static RewardTeamMemberEntity getRewardTeamMemberEntity(RewardTeamEntity teamEntity,
                                                                  RewardTeamMember rewardTeamMember) {
    if (rewardTeamMember == null) {
      return null;
    }
    RewardTeamMemberEntity teamMemberEntity = new RewardTeamMemberEntity();
    teamMemberEntity.setId(rewardTeamMember.getTechnicalId() == null
        || rewardTeamMember.getTechnicalId() == 0 ? null : rewardTeamMember.getTechnicalId());
    teamMemberEntity.setIdentityId(rewardTeamMember.getIdentityId());
    teamMemberEntity.setTeam(teamEntity);
    return teamMemberEntity;
  }

  private static RewardTeamMember getRewardTeamMember(RewardTeamMemberEntity teamMemberEntity) {
    if (teamMemberEntity == null) {
      return null;
    }
    RewardTeamMember rewardTeamMember = getRewardTeamMember(teamMemberEntity.getIdentityId());
    if (rewardTeamMember == null) {
      return null;
    }
    rewardTeamMember.setTechnicalId(teamMemberEntity.getId());
    return rewardTeamMember;
  }

  private static RewardTeamMember getRewardTeamMember(Long identityId) {
    if (identityId == null || identityId == 0) {
      return null;
    }
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    Identity identity = identityManager.getIdentity(String.valueOf(identityId), true);
    if (identity == null) {
      return null;
    }
    RewardTeamMember rewardTeamMember = new RewardTeamMember();
    rewardTeamMember.setId(identity.getRemoteId());
    rewardTeamMember.setProviderId(identity.getProviderId());
    rewardTeamMember.setIdentityId(Long.parseLong(identity.getId()));
    return rewardTeamMember;
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
