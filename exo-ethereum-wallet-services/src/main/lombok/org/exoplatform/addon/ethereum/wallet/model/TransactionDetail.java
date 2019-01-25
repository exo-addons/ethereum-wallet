package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TransactionDetail implements Serializable, Cloneable {

  private static final long serialVersionUID = 658273092293607458L;

  private long              id;

  private long              networkId;

  private String            hash;

  private String            contractAddress;

  private String            contractMethodName;

  private boolean           pending;

  private boolean           succeeded;

  private boolean           isAdminOperation;

  private String            from;

  private String            to;

  private String            label;

  private String            message;

  private double            value;

  private double            contractAmount;

  private long              timestamp;

  @Override
  public TransactionDetail clone() { // NOSONAR
    try {
      return (TransactionDetail) super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }
}
