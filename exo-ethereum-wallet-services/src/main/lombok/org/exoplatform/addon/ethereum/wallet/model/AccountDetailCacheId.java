package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class AccountDetailCacheId implements Serializable {

  private static final long serialVersionUID = -1118998607926773632L;

  String                    id;

  String                    type;

  String                    address;

  public AccountDetailCacheId(String type, String id) {
    this.type = type;
    this.id = id;
  }

  public AccountDetailCacheId(String address) {
    this.address = address;
  }
}
