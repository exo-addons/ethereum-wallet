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
package org.exoplatform.addon.ethereum.wallet.reward.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.addon.ethereum.wallet.reward.model.*;
import org.exoplatform.addon.ethereum.wallet.reward.plugin.RewardPlugin;
import org.exoplatform.ws.frameworks.json.impl.JsonException;

/**
 * A storage service to save/load reward settings
 */
public class RewardService {

  private RewardSettingsService rewardSettingsService;

  private RewardTeamService     rewardTeamService;

  public RewardService(RewardSettingsService rewardSettingsService, RewardTeamService rewardTeamService) {
    this.rewardSettingsService = rewardSettingsService;
    this.rewardTeamService = rewardTeamService;
  }

  public void computeReward(Set<Long> identityIds, Date periodDate) {
    Collection<RewardPlugin> rewardPlugins = rewardSettingsService.getRewardPlugins();
    RewardSettings rewardSettings;
    try {
      rewardSettings = rewardSettingsService.getSettings();
    } catch (JsonException e) {
      throw new IllegalStateException("Can't retrieve reward settings");
    }
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
    RewardPeriod periodOfTime = periodType.getPeriodOfTime(LocalDateTime.ofEpochSecond(periodDate.getTime() / 1000,
                                                                                       0,
                                                                                       ZoneOffset.UTC));

    Set<RewardMemberDetail> rewardMemberDetails = new HashSet<>();
    for (RewardPlugin rewardPlugin : rewardPlugins) {
      if (rewardPlugin == null || !rewardPlugin.isEnabled()) {
        continue;
      }
      Map<Long, Double> earnedPoints = rewardPlugin.gtEarnedPoints(identityIds,
                                                                   periodOfTime.getStartDateInSeconds(),
                                                                   periodOfTime.getEndDateInSeconds());
      RewardPluginSettings rewardPluginSettings = getPluginSetting(pluginSettings, rewardPlugin.getPluginId());
      computeBudget(rewardPluginSettings, earnedPoints, rewardMemberDetails);
    }
  }

  private RewardPluginSettings getPluginSetting(Set<RewardPluginSettings> pluginSettings, String pluginId) {
    for (RewardPluginSettings rewardPluginSettings : pluginSettings) {
      if (StringUtils.equals(pluginId, rewardPluginSettings.getPluginId())) {
        return rewardPluginSettings;
      }
    }
    return null;
  }

  private void computeBudget(RewardPluginSettings rewardPluginSettings,
                             Map<Long, Double> earnedPoints,
                             Set<RewardMemberDetail> rewardMemberDetails) {
    RewardBudgetType budgetType = rewardPluginSettings.getBudgetType();
    String pluginId = rewardPluginSettings.getPluginId();
    double threshold = rewardPluginSettings.getThreshold();
    double configuredPluginAmount = rewardPluginSettings.getAmount();
    if (configuredPluginAmount < 0) {
      throw new IllegalStateException("Plugin with id " + pluginId + " has a configured negative reward amount ("
          + configuredPluginAmount + ")");
    }

    Set<Entry<Long, Double>> identitiesPointsEntries = earnedPoints.entrySet();
    Iterator<Entry<Long, Double>> identitiesPointsIterator = identitiesPointsEntries.iterator();
    while (identitiesPointsIterator.hasNext()) {
      Map.Entry<java.lang.Long, java.lang.Double> entry = identitiesPointsIterator.next();
      if (entry.getValue() < threshold) {
        // Member doesn't have enough points, so he's not eligible
        identitiesPointsIterator.remove();
        if (entry.getValue() > 0) {
          Long identityId = entry.getKey();
          Double points = entry.getValue();

          // Add member with earned points for information on UI
          RewardMemberDetail rewardMemberDetail = new RewardMemberDetail();
          rewardMemberDetail.setIdentityId(identityId);
          rewardMemberDetail.setPluginId(pluginId);
          rewardMemberDetail.setPoints(points);
          rewardMemberDetail.setAmount(0);
        }
      }
    }

    switch (budgetType) {
    case FIXED_PER_POINT:
      for (Entry<Long, Double> identitiyPointsEntry : identitiesPointsEntries) {
        Long identityId = identitiyPointsEntry.getKey();
        Double points = identitiyPointsEntry.getValue();

        RewardMemberDetail rewardMemberDetail = new RewardMemberDetail();
        rewardMemberDetail.setIdentityId(identityId);
        rewardMemberDetail.setPluginId(pluginId);
        rewardMemberDetail.setPoints(points);
        rewardMemberDetail.setAmount(points * configuredPluginAmount);
        rewardMemberDetails.add(rewardMemberDetail);
      }
      break;
    case FIXED:
      if (rewardPluginSettings.isUsePools()) {
        double totalTeamsBudget = configuredPluginAmount;
        List<RewardTeam> teams = rewardTeamService.getTeams();
        Set<Long> identityIds = filterEligibleMembersAndTeams(teams, earnedPoints);
        buildNoPoolUsers(earnedPoints, teams, identityIds);
        computeTeamsMembersBudget(pluginId, teams, totalTeamsBudget, rewardMemberDetails, earnedPoints);
      } else {
        double totalPoints = identitiesPointsEntries.stream().collect(Collectors.summingDouble(entry -> entry.getValue()));
        if (totalPoints <= 0 || configuredPluginAmount == 0) {
          return;
        }
        double amountPerPoint = configuredPluginAmount / totalPoints;
        for (Entry<Long, Double> identitiyPointsEntry : identitiesPointsEntries) {
          Long identityId = identitiyPointsEntry.getKey();
          Double points = identitiyPointsEntry.getValue();
          if (points < 0) {
            throw new IllegalStateException("Plugin with id " + pluginId + " has assigned a negative points (" + points
                + ") to user with id " + identityId);
          }
          RewardMemberDetail rewardMemberDetail = new RewardMemberDetail();
          rewardMemberDetail.setIdentityId(identityId);
          rewardMemberDetail.setPluginId(pluginId);
          rewardMemberDetail.setPoints(points);
          rewardMemberDetail.setAmount(points * amountPerPoint);
          rewardMemberDetails.add(rewardMemberDetail);
        }
      }
      break;
    case FIXED_PER_MEMBER:
      if (rewardPluginSettings.isUsePools()) {
        List<RewardTeam> teams = rewardTeamService.getTeams();
        Set<Long> identityIds = filterEligibleMembersAndTeams(teams, earnedPoints);
        buildNoPoolUsers(earnedPoints, teams, identityIds);

        int totalElligibleMembersCount = teams.stream().collect(Collectors.summingInt(team -> team.getMembers().size()));
        double totalTeamsBudget = configuredPluginAmount * totalElligibleMembersCount;
        computeTeamsMembersBudget(pluginId, teams, totalTeamsBudget, rewardMemberDetails, earnedPoints);
      } else {
        double totalPoints = identitiesPointsEntries.stream().collect(Collectors.summingDouble(entry -> entry.getValue()));
        if (totalPoints <= 0 || configuredPluginAmount == 0) {
          return;
        }
        double amountPerPoint = configuredPluginAmount / totalPoints;
        for (Entry<Long, Double> identitiyPointsEntry : identitiesPointsEntries) {
          Long identityId = identitiyPointsEntry.getKey();
          Double points = identitiyPointsEntry.getValue();
          if (points < 0) {
            throw new IllegalStateException("Plugin with id " + pluginId + " has assigned a negative points (" + points
                + ") to user with id " + identityId);
          }
          RewardMemberDetail rewardMemberDetail = new RewardMemberDetail();
          rewardMemberDetail.setIdentityId(identityId);
          rewardMemberDetail.setPluginId(pluginId);
          rewardMemberDetail.setPoints(points);
          rewardMemberDetail.setAmount(points * amountPerPoint);
          rewardMemberDetails.add(rewardMemberDetail);
        }
      }
      break;
    default:
      throw new IllegalStateException("Budget type is not recognized in plugin settings: " + pluginId
          + ", budget type = " + budgetType);
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

    // Compute fixed budget teams
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
        double totalFixedBudgetForTeam = rewardTeam.getBudget() * rewardTeam.getMembers().size();
        addTeamRewardRepartition(rewardTeam,
                                 totalFixedBudgetForTeam,
                                 totalTeamPoints,
                                 pluginId,
                                 earnedPoints,
                                 rewardMemberDetails);
        totalFixedTeamsBudget += totalFixedBudgetForTeam;
      } else if (teamBudgetType == RewardBudgetType.FIXED) {
        addTeamRewardRepartition(rewardTeam,
                                 rewardTeam.getBudget(),
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

    if (computedRecipientsCount > 0 && !computedBudgetTeams.isEmpty()) {
      double remaingBudgetForComputedTeams = totalTeamsBudget - totalFixedTeamsBudget;
      double budgetPerTeamMember = remaingBudgetForComputedTeams / computedRecipientsCount;
      computedBudgetTeams.forEach(rewardTeam -> {
        double teamBudget = budgetPerTeamMember * rewardTeam.getMembers().size();
        addTeamRewardRepartition(rewardTeam,
                                 teamBudget,
                                 totalPointsPerTeam.get(rewardTeam.getId()),
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
                                        double totalFixedBudgetForTeam,
                                        double totalTeamPoints,
                                        String pluginId,
                                        Map<Long, Double> earnedPoints,
                                        Set<RewardMemberDetail> rewardMemberDetails) {
    if (rewardTeam.getMembers().isEmpty() || totalFixedBudgetForTeam <= 0 || totalTeamPoints <= 0) {
      return;
    }

    double amountPerPoint = totalFixedBudgetForTeam / totalTeamPoints;
    rewardTeam.getMembers().forEach(member -> {
      Long identityId = member.getIdentityId();
      Double points = earnedPoints.get(identityId);

      RewardMemberDetail rewardMemberDetail = new RewardMemberDetail();
      rewardMemberDetail.setIdentityId(identityId);
      rewardMemberDetail.setPluginId(pluginId);
      rewardMemberDetail.setPoints(points);
      rewardMemberDetail.setAmount(points * amountPerPoint);
      rewardMemberDetails.add(rewardMemberDetail);
    });
  }

}
