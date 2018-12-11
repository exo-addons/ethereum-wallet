package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.*;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ObjectFactory;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.rest.resource.AbstractResourceDescriptor;

public class WalletGamificationService {

  private static final Log           LOG                      = ExoLogger.getLogger(WalletGamificationService.class);

  private SettingService             settingService;

  private ResourceBinder             restResourceBinder;

  private GamificationSettings       gamificationSettings     = new GamificationSettings();

  private List<GamificationTeam>     teams                    = new ArrayList<>();

  private AbstractResourceDescriptor gamificationRestResource;

  private boolean                    gamificationRestSearched = false;

  private long                       id                       = 1;

  public WalletGamificationService(ResourceBinder restResourceBinder, SettingService settingService) {
    this.settingService = settingService;
    this.restResourceBinder = restResourceBinder;
  }

  public GamificationSettings getSettings() {
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
      return gamificationSettings;
    } else {
      return null;
    }
  }

  public void saveSettings(GamificationSettings gamificationSettings) {
    this.gamificationSettings = gamificationSettings;
  }

  public List<GamificationTeam> getTeams() {
    return teams;
  }

  public GamificationTeam saveTeam(GamificationTeam gamificationTeam) {
    if (gamificationTeam == null) {
      throw new IllegalArgumentException("Empty team to save");
    }
    if (gamificationTeam.getId() == null || gamificationTeam.getId() == 0) {
      gamificationTeam.setId(id++);
      teams.add(gamificationTeam);
    } else {
      GamificationTeam oldGamificationTeam = teams.stream()
                                                  .filter(team -> team.getId() == gamificationTeam.getId())
                                                  .findFirst()
                                                  .get();
      if (oldGamificationTeam == null) {
        teams.add(gamificationTeam);
      } else {
        teams.remove(oldGamificationTeam);
        teams.add(gamificationTeam);
      }
    }
    return gamificationTeam;
  }

  /**
   * Remove a Gamification Team/Pool by id
   * @param id
   */
  public void removeTeam(long id) {
    if (id != 0) {
      teams = teams.stream().filter(team -> team.getId() != id).collect(Collectors.toList());
    }
  }

  /**
   * @param networkId
   * @param periodType
   * @param startDateInSeconds
   * @return
   */
  public List<JSONObject> getPeriodTransactions(Long networkId, String periodType, long startDateInSeconds) {
    String paramName = getPeriodTransactionsParamName(periodType, startDateInSeconds);
    SettingValue<?> transactionsValue = settingService.get(EXT_WALLET_CONTEXT, EXT_WALLET_SCOPE, paramName);
    String transactionsString = transactionsValue == null ? "" : transactionsValue.getValue().toString();
    String[] transactionsArray = transactionsString.isEmpty() ? new String[0] : transactionsString.split(",");
    return Arrays.stream(transactionsArray).map(transaction -> {
      GamificationTransaction gamificationTransaction = GamificationTransaction.fromStoredValue(transaction);
      gamificationTransaction.setNetworkId(networkId);
      return gamificationTransaction.toJSONObject();
    }).collect(Collectors.toList());
  }

  /**
   * Save gamification transaction
   * 
   * @param gamificationTransaction
   */
  public void savePeriodTransaction(GamificationTransaction gamificationTransaction) {
    if (gamificationTransaction == null) {
      throw new IllegalArgumentException("gamificationTransaction parameter is mandatory");
    }
    if (gamificationTransaction.getNetworkId() == 0) {
      throw new IllegalArgumentException("transaction NetworkId parameter is mandatory");
    }
    if (StringUtils.isBlank(gamificationTransaction.getHash())) {
      throw new IllegalArgumentException("transaction hash parameter is mandatory");
    }
    if (StringUtils.isBlank(gamificationTransaction.getPeriodType())) {
      throw new IllegalArgumentException("transaction PeriodType parameter is mandatory");
    }
    if (gamificationTransaction.getStartDateInSeconds() == 0) {
      throw new IllegalArgumentException("transaction 'period start date' parameter is mandatory");
    }
    if (StringUtils.isBlank(gamificationTransaction.getReceiverType())) {
      throw new IllegalArgumentException("transaction ReceiverType parameter is mandatory");
    }
    if (StringUtils.isBlank(gamificationTransaction.getReceiverId())) {
      throw new IllegalArgumentException("transaction ReceiverId parameter is mandatory");
    }
    if (StringUtils.isBlank(gamificationTransaction.getReceiverIdentityId())) {
      throw new IllegalArgumentException("transaction ReceiverIdentityId parameter is mandatory");
    }

    String paramName = getPeriodTransactionsParamName(gamificationTransaction.getPeriodType(),
                                                      gamificationTransaction.getStartDateInSeconds());
    SettingValue<?> transactionsValue = settingService.get(EXT_WALLET_CONTEXT, EXT_WALLET_SCOPE, paramName);
    String transactionsString = transactionsValue == null ? "" : transactionsValue.getValue().toString();

    if (!transactionsString.contains(gamificationTransaction.getHash())) {
      String contentToPrepend = gamificationTransaction.getToStoreValue();
      transactionsString = transactionsString.isEmpty() ? contentToPrepend : contentToPrepend + "," + transactionsString;
      settingService.set(EXT_WALLET_CONTEXT, EXT_WALLET_SCOPE, paramName, SettingValue.create(transactionsString));
    }
  }

  private String getPeriodTransactionsParamName(String periodType, long startDateInSeconds) {
    return GAMIFICATION_PERIOD_TRANSACTIONS_NAME + periodType + startDateInSeconds;
  }
}
