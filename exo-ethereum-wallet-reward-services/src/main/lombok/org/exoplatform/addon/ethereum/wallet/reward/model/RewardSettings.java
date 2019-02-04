package org.exoplatform.addon.ethereum.wallet.reward.model;

import java.io.Serializable;
import java.util.Set;

import lombok.Data;

@Data
public class RewardSettings implements Serializable {

  private static final long         serialVersionUID = -8650247964730374760L;

  private String                    contractAddress;

  private RewardPeriodType          periodType       = RewardPeriodType.DEFAULT;

  private Set<RewardPluginSettings> pluginSettings;

}
