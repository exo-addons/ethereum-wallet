package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressLabel implements Serializable {
  private static final long serialVersionUID = 3841906887968752979L;

  private long              id;

  @Exclude
  private boolean           isPublic;

  @Exclude
  private String            label;

  @Exclude
  private String            address;

}
