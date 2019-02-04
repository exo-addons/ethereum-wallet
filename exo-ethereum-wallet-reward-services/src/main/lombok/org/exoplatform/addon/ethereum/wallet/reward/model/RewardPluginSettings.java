package org.exoplatform.addon.ethereum.wallet.reward.model;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;

@Data
public class RewardPluginSettings implements Serializable {

  private static final long serialVersionUID = -843790790474775405L;

  private String            pluginId;

  @Exclude
  private boolean           enabled;

  @Exclude
  private double            threshold;

  @Exclude
  private boolean           usePools;

  @Exclude
  private RewardBudgetType  budgetType;

  @Exclude
  private double            amount;

}
