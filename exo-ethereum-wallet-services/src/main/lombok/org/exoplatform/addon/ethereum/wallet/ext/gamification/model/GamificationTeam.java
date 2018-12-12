package org.exoplatform.addon.ethereum.wallet.ext.gamification.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GamificationTeam implements Serializable {

  private static final long            serialVersionUID = 4475704534821391132L;

  private Long                         id;

  private String                       name;

  private String                       description;

  private GamificationRewardType   rewardType;

  private Double                       budget;

  private Double                       rewardPerMember;

  private Long                         spaceId;

  private String                       spacePrettyName;

  private GamificationTeamMember       manager;

  private List<GamificationTeamMember> members          = null;

}
