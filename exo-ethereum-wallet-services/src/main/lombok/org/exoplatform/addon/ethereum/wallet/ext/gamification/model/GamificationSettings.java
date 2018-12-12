package org.exoplatform.addon.ethereum.wallet.ext.gamification.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GamificationSettings {

  long                           threshold;

  private String                 contractAddress;

  private GamificationPeriodType periodType = GamificationPeriodType.DEFAULT;

  private GamificationRewardType rewardType = GamificationRewardType.FIXED;

  private double                 totalBudget;

  private double                 budgetPerMember;

}
