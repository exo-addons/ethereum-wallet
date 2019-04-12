package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TransactionDetail implements Serializable, Cloneable {

  private static final long serialVersionUID = 658273092293607458L;

  private long              id;

  private long              issuerIdentityId;

  private long              networkId;

  private String            hash;

  private String            contractAddress;

  private String            contractMethodName;

  private boolean           pending;

  private boolean           succeeded;

  private boolean           isAdminOperation;

  private String            from;

  private Wallet            fromWallet;

  private String            to;

  private Wallet            toWallet;

  private String            by;

  private Wallet            byWallet;

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
      throw new IllegalStateException("Error while cloning object", e);
    }
  }

  public BigInteger getContractAmountDecimal(int decimals) {
    if (contractAmount == 0) {
      return BigInteger.ZERO;
    }
    BigInteger amountBI = BigDecimal.valueOf(contractAmount).toBigInteger();
    return amountBI.multiply(BigInteger.valueOf(10).pow(decimals));
  }

  public BigInteger getValueDecimal(int decimals) {
    if (value == 0) {
      return BigInteger.ZERO;
    }
    BigInteger amountBI = BigDecimal.valueOf(value).toBigInteger();
    return amountBI.multiply(BigInteger.valueOf(10).pow(decimals));
  }

  public void setContractAmountDecimal(BigInteger amount, int decimals) {
    if (amount == null) {
      this.contractAmount = 0;
      return;
    }
    this.contractAmount = BigDecimal.valueOf(amount.doubleValue()).divide(BigDecimal.valueOf(10).pow(decimals)).doubleValue();
  }

  public void setValueDecimal(BigInteger amount, int decimals) {
    if (amount == null) {
      this.value = 0;
    } else {
      this.value = BigDecimal.valueOf(amount.doubleValue()).divide(BigDecimal.valueOf(10).pow(decimals)).doubleValue();
    }
  }

}
