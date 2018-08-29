package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class ContractDetail implements Serializable {

  private static final long serialVersionUID = 1459881604949041768L;

  private String            address;

  private String            name;

  private String            symbol;

  public ContractDetail() {
  }

  public ContractDetail(String address, String name, String symbol) {
    this.address = address;
    this.name = name;
    this.symbol = symbol;
  }
}
