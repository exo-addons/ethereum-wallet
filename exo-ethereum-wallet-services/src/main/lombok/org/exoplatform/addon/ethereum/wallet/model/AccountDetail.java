package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class AccountDetail implements Serializable {

  private static final long serialVersionUID = 8011288624609310945L;

  private String            id;

  private String            technicalId;

  private String            type;

  private String            name;

  private String            address;

  private String[]          managers;

  private String            avatar;

  public AccountDetail() {
  }

  public AccountDetail(String id,
                       String technicalId,
                       String type,
                       String name,
                       String address,
                       String[] managers,
                       String avatar) {
    super();
    this.id = id;
    this.technicalId = technicalId;
    this.type = type;
    this.name = name;
    this.address = address;
    this.managers = managers;
    this.avatar = avatar;
  }
}
