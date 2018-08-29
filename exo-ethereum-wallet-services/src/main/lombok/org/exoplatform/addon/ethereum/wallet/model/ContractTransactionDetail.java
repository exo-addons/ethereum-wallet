package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class ContractTransactionDetail implements Serializable {

  private static final long serialVersionUID = 870553638242527135L;

  private String            contractAddress;

  private String            sender;

  private String            receiver;

  private double            amount;

  public ContractTransactionDetail() {
  }

  public ContractTransactionDetail(String contractAddress, String sender, String receiver, double amount) {
    this.contractAddress = contractAddress;
    this.sender = sender;
    this.receiver = receiver;
    this.amount = amount;
  }
}
