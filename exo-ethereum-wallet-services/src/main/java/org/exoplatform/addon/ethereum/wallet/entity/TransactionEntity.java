package org.exoplatform.addon.ethereum.wallet.entity;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "WalletTransaction")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_TRANSACTION")
@NamedQueries({
    @NamedQuery(name = "WalletTransaction.getContractTransactions", query = "SELECT tx FROM WalletTransaction tx WHERE tx.networkId = :networkId AND (tx.contractAddress = :contractAddress OR tx.toAddress = :contractAddress) ORDER BY tx.createdDate DESC"),
    @NamedQuery(name = "WalletTransaction.getPendingTransactions", query = "SELECT tx FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.isPending = TRUE"),
    @NamedQuery(name = "WalletTransaction.getTransactionByHash", query = "SELECT tx FROM WalletTransaction tx WHERE tx.hash = :hash"),
    @NamedQuery(name = "WalletTransaction.getAddressLastPendingTransactionSent", query = "SELECT tx FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.fromAddress = :address AND tx.isPending = TRUE ORDER BY tx.createdDate DESC"),
    @NamedQuery(name = "WalletTransaction.getEtherTransactions", query = "SELECT tx FROM WalletTransaction tx WHERE tx.value > 0"),
})
public class TransactionEntity implements Serializable {

  private static final long serialVersionUID = 485850826850947238L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_TRANSACTION", sequenceName = "SEQ_WALLET_TRANSACTION")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_TRANSACTION")
  @Column(name = "TRANSACTION_ID")
  private long              id;

  @Column(name = "NETWORK_ID", nullable = false)
  private long              networkId;

  @Column(name = "ISSUER_ID")
  private long              issuerIdentityId;

  @Column(name = "HASH", unique = true, nullable = false)
  private String            hash;

  @Column(name = "PENDING")
  private boolean           isPending;

  @Column(name = "SUCCESS")
  private boolean           isSuccess;

  @Column(name = "ADMIN_OP")
  private boolean           isAdminOperation;

  @Column(name = "FROM_ADDRESS", nullable = false)
  private String            fromAddress;

  @Column(name = "TO_ADDRESS")
  private String            toAddress;

  @Column(name = "BY_ADDRESS")
  private String            byAddress;

  @Column(name = "LABEL")
  private String            label;

  @Column(name = "MESSAGE")
  private String            message;

  @Column(name = "VALUE")
  private double            value;

  @Column(name = "CONTRACT_ADDRESS")
  private String            contractAddress;

  @Column(name = "CONTRACT_METHOD")
  private String            contractMethodName;

  @Column(name = "CONTRACT_AMOUNT")
  private double            contractAmount;

  @Column(name = "CREATED_DATE")
  private long              createdDate;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getNetworkId() {
    return networkId;
  }

  public void setNetworkId(long networkId) {
    this.networkId = networkId;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public boolean isPending() {
    return isPending;
  }

  public void setPending(boolean isPending) {
    this.isPending = isPending;
  }

  public boolean isSuccess() {
    return isSuccess;
  }

  public void setSuccess(boolean isSuccess) {
    this.isSuccess = isSuccess;
  }

  public boolean isAdminOperation() {
    return isAdminOperation;
  }

  public void setAdminOperation(boolean isAdminOperation) {
    this.isAdminOperation = isAdminOperation;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public String getToAddress() {
    return toAddress;
  }

  public String getByAddress() {
    return byAddress;
  }

  public void setByAddress(String byAddress) {
    this.byAddress = byAddress;
  }

  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public String getContractMethodName() {
    return contractMethodName;
  }

  public void setContractMethodName(String contractMethodName) {
    this.contractMethodName = contractMethodName;
  }

  public double getContractAmount() {
    return contractAmount;
  }

  public void setContractAmount(double contractAmount) {
    this.contractAmount = contractAmount;
  }

  public long getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(long createdDate) {
    this.createdDate = createdDate;
  }

  public long getIssuerIdentityId() {
    return issuerIdentityId;
  }

  public void setIssuerIdentityId(long issuerIdentityId) {
    this.issuerIdentityId = issuerIdentityId;
  }

}
