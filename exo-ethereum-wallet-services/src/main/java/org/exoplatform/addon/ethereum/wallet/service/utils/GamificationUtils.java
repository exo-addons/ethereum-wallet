package org.exoplatform.addon.ethereum.wallet.service.utils;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import org.exoplatform.addon.ethereum.wallet.ext.gamification.entity.GamificationTeamEntity;
import org.exoplatform.addon.ethereum.wallet.ext.gamification.entity.GamificationTeamMemberEntity;
import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.GamificationTeam;
import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.GamificationTeamMember;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.ws.frameworks.json.JsonGenerator;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;

public class GamificationUtils {
  public static final JsonParser    JSON_PARSER    = new JsonParserImpl();

  public static final JsonGenerator JSON_GENERATOR = new JsonGeneratorImpl();

  private GamificationUtils() {
  }

  public static LocalDateTime timeFromSeconds(long createdDate) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(createdDate), TimeZone.getDefault().toZoneId());
  }

  public static long timeToSeconds(LocalDateTime time) {
    return time.atZone(ZoneOffset.systemDefault()).toEpochSecond();
  }

  public static GamificationTeamEntity fromDTO(GamificationTeam gamificationTeam) {
    if (gamificationTeam == null) {
      return null;
    }
    GamificationTeamEntity teamEntity = new GamificationTeamEntity();
    teamEntity.setId(gamificationTeam.getId() == null || gamificationTeam.getId() == 0 ? null : gamificationTeam.getId());
    teamEntity.setName(gamificationTeam.getName());
    teamEntity.setDescription(gamificationTeam.getDescription());
    teamEntity.setBudget(gamificationTeam.getBudget());
    teamEntity.setRewardType(gamificationTeam.getRewardType());
    teamEntity.setRewardPerMember(gamificationTeam.getRewardPerMember());
    teamEntity.setDisabled(gamificationTeam.isDisabled());
    if (gamificationTeam.getManager() != null && gamificationTeam.getManager().getIdentityId() != 0) {
      teamEntity.setManager(gamificationTeam.getManager().getIdentityId());
    }
    if (gamificationTeam.getSpaceId() != null && gamificationTeam.getSpaceId() != 0) {
      teamEntity.setSpaceId(gamificationTeam.getSpaceId());
    }
    if (gamificationTeam.getMembers() != null && !gamificationTeam.getMembers().isEmpty()) {
      teamEntity.setMembers(gamificationTeam.getMembers()
                                            .stream()
                                            .map(gamificationTeamMember -> getGamificationTeamMemberEntity(teamEntity,
                                                                                                           gamificationTeamMember))
                                            .collect(Collectors.toSet()));
    }
    return teamEntity;
  }

  public static GamificationTeam toDTO(GamificationTeamEntity teamEntity) {
    if (teamEntity == null) {
      return null;
    }
    GamificationTeam gamificationTeam = new GamificationTeam();
    gamificationTeam.setId(teamEntity.getId());
    gamificationTeam.setName(teamEntity.getName());
    gamificationTeam.setDescription(teamEntity.getDescription());
    gamificationTeam.setBudget(teamEntity.getBudget());
    gamificationTeam.setManager(getGamificationTeamMember(teamEntity.getManager()));
    gamificationTeam.setRewardType(teamEntity.getRewardType());
    gamificationTeam.setRewardPerMember(teamEntity.getRewardPerMember());
    gamificationTeam.setDisabled(teamEntity.getDisabled());
    if (teamEntity.getSpaceId() != null && teamEntity.getSpaceId() != 0) {
      SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
      Space space = spaceService.getSpaceById(String.valueOf(teamEntity.getSpaceId()));
      if (space != null) {
        gamificationTeam.setSpaceId(teamEntity.getSpaceId());
        gamificationTeam.setSpacePrettyName(space.getPrettyName());
      }
    }
    if (teamEntity.getMembers() != null && !teamEntity.getMembers().isEmpty()) {
      List<GamificationTeamMember> list = teamEntity.getMembers()
                                                    .stream()
                                                    .map(teamMemberEntity -> getGamificationTeamMember(teamMemberEntity))
                                                    .collect(Collectors.toList());
      gamificationTeam.setMembers(new ArrayList<>(list));
    }
    return gamificationTeam;
  }

  private static GamificationTeamMemberEntity getGamificationTeamMemberEntity(GamificationTeamEntity teamEntity,
                                                                              GamificationTeamMember gamificationTeamMember) {
    if (gamificationTeamMember == null) {
      return null;
    }
    GamificationTeamMemberEntity teamMemberEntity = new GamificationTeamMemberEntity();
    teamMemberEntity.setId(gamificationTeamMember.getTechnicalId() == null
        || gamificationTeamMember.getTechnicalId() == 0 ? null : gamificationTeamMember.getTechnicalId());
    teamMemberEntity.setIdentityId(gamificationTeamMember.getIdentityId());
    teamMemberEntity.setTeam(teamEntity);
    return teamMemberEntity;
  }

  private static GamificationTeamMember getGamificationTeamMember(GamificationTeamMemberEntity teamMemberEntity) {
    if (teamMemberEntity == null) {
      return null;
    }
    GamificationTeamMember gamificationTeamMember = getGamificationTeamMember(teamMemberEntity.getIdentityId());
    if (gamificationTeamMember == null) {
      return null;
    }
    gamificationTeamMember.setTechnicalId(teamMemberEntity.getId());
    return gamificationTeamMember;
  }

  private static GamificationTeamMember getGamificationTeamMember(Long identityId) {
    if (identityId == null || identityId == 0) {
      return null;
    }
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    Identity identity = identityManager.getIdentity(String.valueOf(identityId), true);
    if (identity == null) {
      return null;
    }
    GamificationTeamMember gamificationTeamMember = new GamificationTeamMember();
    gamificationTeamMember.setId(identity.getRemoteId());
    gamificationTeamMember.setProviderId(identity.getProviderId());
    gamificationTeamMember.setIdentityId(Long.parseLong(identity.getId()));
    return gamificationTeamMember;
  }
}
