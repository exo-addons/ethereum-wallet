package org.exoplatform.addon.ethereum.wallet.ext.gamification.model;

import lombok.Data;

@Data
public class GamificationSettings {

  long                   threshold;

  private String         contractAddress;

  GamificationPeriodType periodType = GamificationPeriodType.DEFAULT;

  private double         totalBudget;

}
