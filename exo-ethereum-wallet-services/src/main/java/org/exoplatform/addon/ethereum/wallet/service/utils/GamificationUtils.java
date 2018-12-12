package org.exoplatform.addon.ethereum.wallet.service.utils;

import java.time.*;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.exoplatform.addon.ethereum.wallet.ext.gamification.entity.GamificationTeamEntity;
import org.exoplatform.addon.ethereum.wallet.ext.gamification.entity.GamificationTeamMemberEntity;
import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class GamificationUtils {
  private static final Log LOG = ExoLogger.getLogger(GamificationUtils.class);

  public static LocalDateTime timeFromSeconds(long createdDate) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(createdDate), TimeZone.getDefault().toZoneId());
  }

  public static long timeToSeconds(LocalDateTime time) {
    return time.atZone(ZoneOffset.systemDefault()).toEpochSecond();
  }

  public static GamificationPeriod getCurrentPeriod(GamificationSettings gamificationSettings) {
    return getPeriodOfTime(gamificationSettings, LocalDateTime.now());
  }

  public static GamificationPeriod getPeriodOfTime(GamificationSettings gamificationSettings, LocalDateTime localDateTime) {
    GamificationPeriodType gamificationPeriodType = null;
    if (gamificationSettings == null || gamificationSettings.getPeriodType() == null) {
      LOG.warn("Provided gamificationSettings doesn't have a parametred gamification period type, using MONTH period type: "
          + gamificationSettings, new RuntimeException());
      gamificationPeriodType = GamificationPeriodType.DEFAULT;
    } else {
      gamificationPeriodType = gamificationSettings.getPeriodType();
    }
    return gamificationPeriodType.getPeriodOfTime(localDateTime);
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
    if (gamificationTeam.getManager() != null && gamificationTeam.getManager().getIdentityId() != 0) {
      teamEntity.setManager(gamificationTeam.getManager().getIdentityId());
    }
    if (gamificationTeam.getSpaceId() != null && gamificationTeam.getSpaceId() != 0) {
      teamEntity.setSpaceId(gamificationTeam.getSpaceId());
    }
    if (gamificationTeam.getMembers() != null && gamificationTeam.getMembers().size() > 0) {
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
    if (teamEntity.getSpaceId() != null && teamEntity.getSpaceId() != 0) {
      SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
      Space space = spaceService.getSpaceById(String.valueOf(teamEntity.getSpaceId()));
      if (space != null) {
        gamificationTeam.setSpaceId(teamEntity.getSpaceId());
        gamificationTeam.setSpacePrettyName(space.getPrettyName());
      }
    }
    if (teamEntity.getMembers() != null && teamEntity.getMembers().size() > 0) {
      gamificationTeam.setMembers(teamEntity.getMembers()
                                            .stream()
                                            .map(teamMemberEntity -> getGamificationTeamMember(teamMemberEntity))
                                            .collect(Collectors.toList()));
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
