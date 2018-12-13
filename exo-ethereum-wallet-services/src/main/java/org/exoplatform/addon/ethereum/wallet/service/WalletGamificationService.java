package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.GamificationUtils.fromDTO;
import static org.exoplatform.addon.ethereum.wallet.service.utils.GamificationUtils.toDTO;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.addon.ethereum.wallet.dao.GamificationTeamDAO;
import org.exoplatform.addon.ethereum.wallet.ext.gamification.entity.GamificationTeamEntity;
import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.GamificationSettings;
import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.GamificationTeam;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ObjectFactory;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.rest.resource.AbstractResourceDescriptor;

public class WalletGamificationService {

  private static final Log           LOG                      = ExoLogger.getLogger(WalletGamificationService.class);

  GamificationTeamDAO                gamificationTeamDAO      = null;

  private SettingService             settingService           = null;

  private ResourceBinder             restResourceBinder       = null;

  private GamificationSettings       gamificationSettings     = null;

  // Used to see if gamification addon is deployed
  private AbstractResourceDescriptor gamificationRestResource = null;

  private boolean                    gamificationRestSearched = false;

  public WalletGamificationService(GamificationTeamDAO gamificationTeamDAO,
                                   ResourceBinder restResourceBinder,
                                   SettingService settingService) {
    this.gamificationTeamDAO = gamificationTeamDAO;
    this.settingService = settingService;
    this.restResourceBinder = restResourceBinder;
  }

  public GamificationSettings getSettings() throws Exception {
    if (this.gamificationRestResource == null && !gamificationRestSearched) {
      try {
        // Check if gamification is deployed
        List<ObjectFactory<AbstractResourceDescriptor>> resources = restResourceBinder.getResources();
        this.gamificationRestResource = resources.stream()
                                                 .map(resource -> resource.getObjectModel())
                                                 .filter(resource -> resource.getPathValue() != null
                                                     && resource.getPathValue().getPath() != null
                                                     && resource.getPathValue().getPath().contains("gamification/api"))
                                                 .findFirst()
                                                 .get();
      } catch (Exception e) {
        LOG.warn("Error getting gamification REST resource", e);
      }
      // Avoid reloading resources even when an exception is thrown
      this.gamificationRestSearched = true;
    }
    if (this.gamificationRestResource != null) {
      if (this.gamificationSettings == null) {
        SettingValue<?> value = settingService.get(EXT_WALLET_CONTEXT, EXT_WALLET_SCOPE, EXT_GAMIFICATION_SETTINGS_KEY_NAME);
        if (value != null && value.getValue() != null) {
          this.gamificationSettings = value == null
              || value.getValue() == null ? this.gamificationSettings
                                          : GamificationSettings.fromString(value.getValue().toString());
        }
      }
      return this.gamificationSettings;
    } else {
      return null;
    }
  }

  public GamificationSettings saveSettings(GamificationSettings gamificationSettings) {
    if (gamificationSettings == null) {
      throw new IllegalArgumentException("settings are empty");
    }
    settingService.set(EXT_WALLET_CONTEXT,
                       EXT_WALLET_SCOPE,
                       EXT_GAMIFICATION_SETTINGS_KEY_NAME,
                       SettingValue.create(gamificationSettings.toStringToStore()));
    this.gamificationSettings = null;
    return gamificationSettings;
  }

  public List<GamificationTeam> getTeams() {
    List<GamificationTeamEntity> teamEntities = gamificationTeamDAO.findAll();
    return teamEntities.stream().map(teamEntity -> toDTO(teamEntity)).collect(Collectors.toList());
  }

  public GamificationTeam saveTeam(GamificationTeam gamificationTeam) {
    if (gamificationTeam == null) {
      throw new IllegalArgumentException("Empty team to save");
    }
    GamificationTeamEntity teamEntity = fromDTO(gamificationTeam);
    if (teamEntity.getId() == null || teamEntity.getId() == 0) {
      teamEntity = gamificationTeamDAO.create(teamEntity);
    } else {
      teamEntity = gamificationTeamDAO.update(teamEntity);
    }
    return toDTO(gamificationTeamDAO.find(teamEntity.getId()));
  }

  /**
   * Remove a Gamification Team/Pool by id
   * 
   * @param id
   * @return
   */
  public GamificationTeam removeTeam(Long id) {
    if (id == null || id == 0) {
      throw new IllegalArgumentException("Team id is required");
    }
    GamificationTeamEntity entity = gamificationTeamDAO.find(id);
    if (entity != null) {
      gamificationTeamDAO.delete(entity);
    }
    return toDTO(entity);
  }
}
