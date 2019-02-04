package org.exoplatform.addon.ethereum.wallet.reward.model;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;

@Data
public class RewardMemberDetail implements Serializable {

  private static final long serialVersionUID = 1622627645862974585L;

  private String            pluginId;

  private long              identityId;

  @Exclude
  private double            points;

  @Exclude
  private double            amount;

}
