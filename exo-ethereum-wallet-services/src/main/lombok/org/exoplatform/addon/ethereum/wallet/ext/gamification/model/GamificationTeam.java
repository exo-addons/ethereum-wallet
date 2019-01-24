package org.exoplatform.addon.ethereum.wallet.ext.gamification.model;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GamificationTeam implements Serializable {

  private static final long                 serialVersionUID = 4475704534821391132L;

  private Long                              id;

  private String                            name;

  private String                            description;

  private GamificationRewardType            rewardType;

  private Double                            budget;

  private Double                            rewardPerMember;

  private Long                              spaceId;

  private String                            spacePrettyName;

  private boolean                           disabled;

  private GamificationTeamMember            manager;

  private ArrayList<GamificationTeamMember> members          = null;

}
