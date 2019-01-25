package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class WalletCacheId implements Serializable {

  private static final long serialVersionUID = -1118998607926773632L;

  String                    id;

  String                    type;

  String                    address;

  public WalletCacheId(String type, String id) {
    this.type = type;
    this.id = id;
  }

  public WalletCacheId(String address) {
    this.address = address;
  }
}
