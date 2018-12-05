package org.exoplatform.addon.ethereum.wallet.ext.gamification.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class GamificationTeam implements Serializable {

  private static final long            serialVersionUID = 4475704534821391132L;

  private long                         id;

  private String                       name;

  private String                       description;

  private double                       budgetInTokens;

  private GamificationTeamMember       owner;

  private List<GamificationTeamMember> members          = null;

}
