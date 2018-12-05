package org.exoplatform.addon.ethereum.wallet.ext.gamification.model;

import java.io.Serializable;

import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;

import lombok.Data;

@Data
public class GamificationTeamMember implements Serializable {

  private static final long serialVersionUID = -2614989453007394487L;

  private String            id;

  private String            providerId       = OrganizationIdentityProvider.NAME;

  private String            identityId;

}
