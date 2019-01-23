package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDetail implements Serializable {

  private static final long serialVersionUID = 8011288624609310945L;

  private String            id;

  @Exclude
  private String            technicalId;

  // A string is used instead of enum, because of cache clustering
  // problems with enums
  private String            type;

  @Exclude
  private String            name;

  @Exclude
  private String            address;

  @Exclude
  private boolean           isSpaceAdministrator;

  @Exclude
  private boolean           isEnabled;

  @Exclude
  private String            avatar;

}
