package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDetail implements Serializable {

  private static final long serialVersionUID = 8011288624609310945L;

  private String            id;

  private String            technicalId;

  // A string is used instead of enum, because of cache clustering
  // problems with enums
  private String            type;

  private String            name;

  private String            address;

  private boolean           isSpaceAdministrator;

  private String            avatar;

}
