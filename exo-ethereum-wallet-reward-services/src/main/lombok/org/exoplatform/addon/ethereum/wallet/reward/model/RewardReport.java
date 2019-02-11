package org.exoplatform.addon.ethereum.wallet.reward.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class RewardReport {

  Set<RewardMemberDetail> rewards = new HashSet<>();

}
