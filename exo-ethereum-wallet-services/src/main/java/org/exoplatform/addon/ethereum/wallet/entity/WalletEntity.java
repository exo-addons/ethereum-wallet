package org.exoplatform.addon.ethereum.wallet.entity;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.addon.ethereum.wallet.model.WalletInitializationState;
import org.exoplatform.addon.ethereum.wallet.model.WalletType;
import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "Wallet")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_ACCOUNT")
@NamedQueries({
    @NamedQuery(name = "Wallet.findByAddress", query = "SELECT w FROM Wallet w WHERE w.address = :address"),
})
public class WalletEntity implements Serializable {
  private static final long         serialVersionUID = -1622032986992776281L;

  @Id
  @Column(name = "IDENTITY_ID")
  private Long                      id;

  @Column(name = "IDENTITY_TYPE", nullable = false)
  private WalletType                type;

  @Column(name = "ADDRESS", unique = true, nullable = false)
  private String                    address;

  @Column(name = "PHRASE", nullable = false)
  private String                    passPhrase;

  @Column(name = "ENABLED", nullable = false)
  private boolean                   isEnabled;

  @Column(name = "INITIALIZATION_STATE")
  private WalletInitializationState initializationState;

  @OneToOne(fetch = FetchType.EAGER, mappedBy = "wallet", cascade = CascadeType.REMOVE)
  private WalletPrivateKeyEntity    privateKey;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public void setEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  public WalletType getType() {
    return type;
  }

  public void setType(WalletType type) {
    this.type = type;
  }

  public String getPassPhrase() {
    return passPhrase;
  }

  public void setPassPhrase(String passPhrase) {
    this.passPhrase = passPhrase;
  }

  public WalletInitializationState getInitializationState() {
    return initializationState;
  }

  public void setInitializationState(WalletInitializationState initializationState) {
    this.initializationState = initializationState;
  }

  public WalletPrivateKeyEntity getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(WalletPrivateKeyEntity privateKey) {
    this.privateKey = privateKey;
  }

}
