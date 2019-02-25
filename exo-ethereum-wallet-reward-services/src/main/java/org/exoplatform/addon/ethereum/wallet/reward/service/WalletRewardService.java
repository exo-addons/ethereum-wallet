/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.addon.ethereum.wallet.reward.service;

import static org.exoplatform.addon.ethereum.wallet.utils.RewardUtils.timeFromSeconds;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.Wallet;
import org.exoplatform.addon.ethereum.wallet.reward.api.RewardPlugin;
import org.exoplatform.addon.ethereum.wallet.reward.model.*;
import org.exoplatform.addon.ethereum.wallet.service.WalletAccountService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A storage service to save/load reward settings
 */
public class WalletRewardService implements RewardService {
  private static final Log      LOG = ExoLogger.getLogger(WalletRewardService.class);

  private RewardSettingsService rewardSettingsService;

  private RewardTeamService     rewardTeamService;

  private WalletAccountService  walletAccountService;

  public WalletRewardService(WalletAccountService walletAccountService,
                             RewardSettingsService rewardSettingsService,
                             RewardTeamService rewardTeamService) {
    this.walletAccountService = walletAccountService;
    this.rewardSettingsService = rewardSettingsService;
    this.rewardTeamService = rewardTeamService;
  }

  @Override
  public Set<RewardMemberDetail> computeReward(Set<Long> identityIds, long periodDateInSeconds) {
    if (periodDateInSeconds == 0) {
      throw new IllegalArgumentException("periodDate is mandatory");
    }
    if (identityIds == null || identityIds.isEmpty()) {
      return Collections.emptySet();
    }
    Collection<RewardPlugin> rewardPlugins = rewardSettingsService.getRewardPlugins();
    RewardSettings rewardSettings;
    rewardSettings = rewardSettingsService.getSettings();
    if (rewardSettings == null) {
      throw new IllegalStateException("Error computing rewards using empty settings");
    }
    if (rewardSettings.getPeriodType() == null) {
      throw new IllegalStateException("Error computing rewards using empty period type");
    }
    Set<RewardPluginSettings> pluginSettings = rewardSettings.getPluginSettings();
    if (pluginSettings == null || pluginSettings.isEmpty()) {
      throw new IllegalStateException("Error computing rewards using empty rewards types");
    }

    RewardPeriodType periodType = rewardSettings.getPeriodType();
    RewardPeriod periodOfTime = periodType.getPeriodOfTime(timeFromSeconds(periodDateInSeconds));

    Set<Long> enabledIdentityIds = getEnabledWallets(identityIds);
    Set<Long> walletsWithEnabledTeam = getEnabledTeamMembers(enabledIdentityIds);

    Set<RewardMemberDetail> rewardMemberDetails = new HashSet<>();
    for (RewardPlugin rewardPlugin : rewardPlugins) {
      if (rewardPlugin == null || !rewardPlugin.isEnabled()) {
        continue;
      }
      RewardPluginSettings rewardPluginSettings = getPluginSetting(pluginSettings, rewardPlugin.getPluginId());
      if (rewardPluginSettings != null) {
        Map<Long, Double> earnedPoints = rewardPlugin.gtEarnedPoints(identityIds,
                                                                     periodOfTime.getStartDateInSeconds(),
                                                                     periodOfTime.getEndDateInSeconds());
        Set<Long> validIdentityIdsToUse = rewardPluginSettings.isUsePools() ? walletsWithEnabledTeam : enabledIdentityIds;
        computeReward(rewardPluginSettings, earnedPoints, validIdentityIdsToUse, rewardMemberDetails);
      }
    }
    return rewardMemberDetails;
  }

  private Set<Long> getEnabledTeamMembers(Set<Long> identityIds) {
    Set<Long> walletsWithEnabledTeam = new HashSet<>(identityIds);
    List<RewardTeam> teams = rewardTeamService.getTeams();
    for (RewardTeam rewardTeam : teams) {
      if (!rewardTeam.isDisabled() || rewardTeam.getMembers() == null || rewardTeam.getMembers().isEmpty()) {
        continue;
      }
      rewardTeam.getMembers().forEach(member -> walletsWithEnabledTeam.remove(member.getIdentityId()));
    }
    return walletsWithEnabledTeam;
  }

  private Set<Long> getEnabledWallets(Set<Long> identityIds) {
    Iterator<Long> identityIdsIterator = identityIds.iterator();
    while (identityIdsIterator.hasNext()) {
      Long identityId = identityIdsIterator.next();
      if (identityId == null || identityId == 0) {
        identityIdsIterator.remove();
      }
      Wallet wallet = null;
      try {
        wallet = walletAccountService.getWalletByIdentityId(identityId);
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.warn("Error while getting wallet of identity with id {}", identityId, e);
        } else {
          LOG.warn("Error while getting wallet of identity with id {}. Reason: {}", identityId, e.getMessage());
        }
      }
      if (wallet == null) {
        identityIdsIterator.remove();
        continue;
      }
      if (!wallet.isEnabled()) {
        identityIdsIterator.remove();
      }
    }
    return identityIds;
  }

  private RewardPluginSettings getPluginSetting(Set<RewardPluginSettings> pluginSettings, String pluginId) {
    for (RewardPluginSettings rewardPluginSettings : pluginSettings) {
      if (StringUtils.equals(pluginId, rewardPluginSettings.getPluginId())) {
        return rewardPluginSettings;
      }
    }
    return null;
  }

  private void computeReward(RewardPluginSettings rewardPluginSettings,
                             Map<Long, Double> earnedPoints,
                             Set<Long> validIdentityIdsToUse,
                             Set<RewardMemberDetail> rewardMemberDetails) {
    RewardBudgetType budgetType = rewardPluginSettings.getBudgetType();
    if (budgetType == null) {
      LOG.warn("Budget type of reward plugin {} is empty, thus no computing is possible", rewardPluginSettings.getPluginId());
      return;
    }
    String pluginId = rewardPluginSettings.getPluginId();
    double configuredPluginAmount = rewardPluginSettings.getAmount();
    if (configuredPluginAmount < 0) {
      throw new IllegalStateException("Plugin " + pluginId + " has a configured negative reward amount ("
          + configuredPluginAmount + ")");
    }

    // Filter non elligible users switch threshold
    filterElligibleMembers(earnedPoints.entrySet(), validIdentityIdsToUse, rewardPluginSettings, rewardMemberDetails);

    double amountPerPoint = 0;
    double totalFixedBudget = 0;
    switch (budgetType) {
    case FIXED_PER_POINT:
      amountPerPoint = configuredPluginAmount;
      addRewardsSwitchPointAmount(rewardMemberDetails, earnedPoints.entrySet(), pluginId, amountPerPoint);
      break;
    case FIXED:
      totalFixedBudget = configuredPluginAmount;
      addTeamMembersReward(rewardPluginSettings, earnedPoints, totalFixedBudget, rewardMemberDetails);
      break;
    case FIXED_PER_MEMBER:
      double budgetPerMember = configuredPluginAmount;
      int totalElligibleMembersCount = earnedPoints.size();
      totalFixedBudget = budgetPerMember * totalElligibleMembersCount;
      addTeamMembersReward(rewardPluginSettings, earnedPoints, totalFixedBudget, rewardMemberDetails);
      break;
    default:
      throw new IllegalStateException("Budget type is not recognized in plugin settings: " + pluginId
          + ", budget type = " + budgetType);
    }
  }

  private void addTeamMembersReward(RewardPluginSettings rewardPluginSettings,
                                    Map<Long, Double> earnedPoints,
                                    double totalFixedBudget,
                                    Set<RewardMemberDetail> rewardMemberDetails) {
    if (totalFixedBudget <= 0) {
      return;
    }
    double amountPerPoint;
    if (rewardPluginSettings.isUsePools()) {
      List<RewardTeam> teams = rewardTeamService.getTeams();
      Set<Long> identityIds = filterEligibleMembersAndTeams(teams, earnedPoints);
      buildNoPoolUsers(earnedPoints, teams, identityIds);
      computeTeamsMembersBudget(rewardPluginSettings.getPluginId(), teams, totalFixedBudget, rewardMemberDetails, earnedPoints);
    } else {
      double totalPoints = earnedPoints.entrySet().stream().collect(Collectors.summingDouble(entry -> entry.getValue()));
      if (totalPoints <= 0 || totalFixedBudget <= 0) {
        return;
      }
      amountPerPoint = totalFixedBudget / totalPoints;
      addRewardsSwitchPointAmount(rewardMemberDetails,
                                  earnedPoints.entrySet(),
                                  rewardPluginSettings.getPluginId(),
                                  amountPerPoint);
    }
  }

  private void addRewardsSwitchPointAmount(Set<RewardMemberDetail> rewardMemberDetails,
                                           Set<Entry<Long, Double>> identitiesPointsEntries,
                                           String pluginId,
                                           double amountPerPoint) {
    for (Entry<Long, Double> identitiyPointsEntry : identitiesPointsEntries) {
      Long identityId = identitiyPointsEntry.getKey();
      Double points = identitiyPointsEntry.getValue();
      double amount = points * amountPerPoint;

      RewardMemberDetail rewardMemberDetail = new RewardMemberDetail();
      rewardMemberDetail.setIdentityId(identityId);
      rewardMemberDetail.setPluginId(pluginId);
      rewardMemberDetail.setPoints(points);
      rewardMemberDetail.setAmount(amount);
      rewardMemberDetail.setPoolsUsed(false);
      rewardMemberDetails.add(rewardMemberDetail);
    }
  }

  private void filterElligibleMembers(Set<Entry<Long, Double>> identitiesPointsEntries,
                                      Set<Long> validIdentityIdsToUse,
                                      RewardPluginSettings rewardPluginSettings,
                                      Set<RewardMemberDetail> rewardMemberDetails) {
    String pluginId = rewardPluginSettings.getPluginId();
    double threshold = rewardPluginSettings.getThreshold();

    Iterator<Entry<Long, Double>> identitiesPointsIterator = identitiesPointsEntries.iterator();
    while (identitiesPointsIterator.hasNext()) {
      Map.Entry<java.lang.Long, java.lang.Double> entry = identitiesPointsIterator.next();
      Long identityId = entry.getKey();
      Double points = entry.getValue();
      points = points == null ? 0 : points;
      if (points < 0) {
        throw new IllegalStateException("Plugin with id " + pluginId + " has assigned a negative points (" + points
            + ") to user with id " + identityId);
      }

      if (points < threshold || points == 0 || !validIdentityIdsToUse.contains(identityId)) {
        // Member doesn't have enough points or his wallet is disabled => not
        // eligible
        identitiesPointsIterator.remove();

        if (points > 0) {
          // Add member with earned points for information on UI
          RewardMemberDetail rewardMemberDetail = new RewardMemberDetail();
          rewardMemberDetail.setIdentityId(identityId);
          rewardMemberDetail.setPluginId(pluginId);
          rewardMemberDetail.setPoints(points);
          rewardMemberDetail.setAmount(0);
          rewardMemberDetail.setPoolsUsed(rewardPluginSettings.isUsePools());
          rewardMemberDetails.add(rewardMemberDetail);
        }
      }
    }
  }

  private void computeTeamsMembersBudget(String pluginId,
                                         List<RewardTeam> teams,
                                         double totalTeamsBudget,
                                         Set<RewardMemberDetail> rewardMemberDetails,
                                         Map<Long, Double> earnedPoints) {
    double totalFixedTeamsBudget = 0;
    double computedRecipientsCount = 0;
    List<RewardTeam> computedBudgetTeams = new ArrayList<>();
    Map<Long, Double> totalPointsPerTeam = new HashMap<>();

    // Compute teams budget with fixed amount
    for (RewardTeam rewardTeam : teams) {
      RewardBudgetType teamBudgetType = rewardTeam.getRewardType();
      double totalTeamPoints = rewardTeam.getMembers()
                                         .stream()
                                         .collect(Collectors.summingDouble(member -> earnedPoints.get(member.getIdentityId())));
      if (teamBudgetType == RewardBudgetType.COMPUTED) {
        computedRecipientsCount += rewardTeam.getMembers().size();
        computedBudgetTeams.add(rewardTeam);
        totalPointsPerTeam.put(rewardTeam.getId(), totalTeamPoints);
      } else if (teamBudgetType == RewardBudgetType.FIXED_PER_MEMBER) {
        double totalTeamBudget = rewardTeam.getBudget() * rewardTeam.getMembers().size();
        addTeamRewardRepartition(rewardTeam,
                                 totalTeamBudget,
                                 totalTeamPoints,
                                 pluginId,
                                 earnedPoints,
                                 rewardMemberDetails);
        totalFixedTeamsBudget += totalTeamBudget;
      } else if (teamBudgetType == RewardBudgetType.FIXED) {
        double totalTeamBudget = rewardTeam.getBudget();
        addTeamRewardRepartition(rewardTeam,
                                 totalTeamBudget,
                                 totalTeamPoints,
                                 pluginId,
                                 earnedPoints,
                                 rewardMemberDetails);
        totalFixedTeamsBudget += rewardTeam.getBudget();
      }
    }

    if (totalFixedTeamsBudget >= totalTeamsBudget) {
      throw new IllegalStateException("Total fixed teams budget is higher than fixed budget for all users");
    }

    // Compute teams budget with computed amount
    if (computedRecipientsCount > 0 && !computedBudgetTeams.isEmpty()) {
      double remaingBudgetForComputedTeams = totalTeamsBudget - totalFixedTeamsBudget;
      double budgetPerTeamMember = remaingBudgetForComputedTeams / computedRecipientsCount;
      computedBudgetTeams.forEach(rewardTeam -> {
        double totalTeamBudget = budgetPerTeamMember * rewardTeam.getMembers().size();
        Double totalTeamPoints = totalPointsPerTeam.get(rewardTeam.getId());
        addTeamRewardRepartition(rewardTeam,
                                 totalTeamBudget,
                                 totalTeamPoints,
                                 pluginId,
                                 earnedPoints,
                                 rewardMemberDetails);
      });
    }
  }

  private void buildNoPoolUsers(Map<Long, Double> earnedPoints, List<RewardTeam> teams, Set<Long> identityIds) {
    // Build "No pool" users
    ArrayList<Long> noPoolsIdentityIds = new ArrayList<>(earnedPoints.keySet());
    noPoolsIdentityIds.removeAll(identityIds);
    if (!noPoolsIdentityIds.isEmpty()) {
      RewardTeam noPoolRewardTeam = new RewardTeam();
      noPoolRewardTeam.setDisabled(false);
      List<RewardTeamMember> noPoolRewardTeamList = noPoolsIdentityIds.stream()
                                                                      .map(identityId -> {
                                                                        RewardTeamMember rewardTeamMember =
                                                                                                          new RewardTeamMember();
                                                                        rewardTeamMember.setIdentityId(identityId);
                                                                        return rewardTeamMember;
                                                                      })
                                                                      .collect(Collectors.toList());
      noPoolRewardTeam.setMembers(noPoolRewardTeamList);
      noPoolRewardTeam.setId(0L);
      noPoolRewardTeam.setRewardType(RewardBudgetType.COMPUTED);
      teams.add(noPoolRewardTeam);
    }
  }

  private Set<Long> filterEligibleMembersAndTeams(List<RewardTeam> teams, Map<Long, Double> earnedPoints) {
    Set<Long> identityIds = new HashSet<>();

    // Search for duplicated users and retain only elligible members in
    // Teams
    Iterator<RewardTeam> teamsIterator = teams.iterator();
    while (teamsIterator.hasNext()) {
      RewardTeam rewardTeam = teamsIterator.next();
      List<RewardTeamMember> members = rewardTeam.getMembers();
      if (members != null && !members.isEmpty()) {
        Iterator<RewardTeamMember> membersIterator = members.iterator();
        while (membersIterator.hasNext()) {
          RewardTeamMember member = membersIterator.next();
          Long identityId = member.getIdentityId();
          if (identityIds.contains(identityId)) {
            throw new IllegalStateException("Team " + rewardTeam.getName() + " has a duplicated member in another Team");
          }
          identityIds.add(identityId);
          // Retain in Teams collection only elligible members
          if (!earnedPoints.containsKey(identityId)) {
            membersIterator.remove();
          }
        }
      }
      if (members == null || members.isEmpty()) {
        teamsIterator.remove();
      }
    }
    return identityIds;
  }

  private void addTeamRewardRepartition(RewardTeam rewardTeam,
                                        double totalTeamBudget,
                                        double totalTeamPoints,
                                        String pluginId,
                                        Map<Long, Double> earnedPoints,
                                        Set<RewardMemberDetail> rewardMemberDetails) {
    if (rewardTeam.getMembers().isEmpty() || totalTeamBudget <= 0 || totalTeamPoints <= 0) {
      return;
    }

    double amountPerPoint = totalTeamBudget / totalTeamPoints;
    rewardTeam.getMembers().forEach(member -> {
      Long identityId = member.getIdentityId();
      Double points = earnedPoints.get(identityId);

      RewardMemberDetail rewardMemberDetail = new RewardMemberDetail();
      rewardMemberDetail.setIdentityId(identityId);
      rewardMemberDetail.setPluginId(pluginId);
      rewardMemberDetail.setPoints(points);
      rewardMemberDetail.setAmount(points * amountPerPoint);
      rewardMemberDetail.setPoolsUsed(true);
      rewardMemberDetails.add(rewardMemberDetail);
    });
  }
}
