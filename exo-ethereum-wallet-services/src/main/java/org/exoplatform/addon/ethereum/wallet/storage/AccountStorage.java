package org.exoplatform.addon.ethereum.wallet.storage;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.getIdentityById;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.getSpace;

import java.util.*;
import java.util.stream.Collectors;

import org.exoplatform.addon.ethereum.wallet.dao.WalletAccountDAO;
import org.exoplatform.addon.ethereum.wallet.entity.WalletEntity;
import org.exoplatform.addon.ethereum.wallet.model.Wallet;
import org.exoplatform.addon.ethereum.wallet.model.WalletType;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;

public class AccountStorage {

  private WalletAccountDAO walletAccountDAO;

  public AccountStorage(WalletAccountDAO walletAccountDAO) {
    this.walletAccountDAO = walletAccountDAO;
  }

  /**
   * Retrieves the list registered wallets
   * 
   * @return
   */
  public Set<Wallet> listWallets() {
    List<WalletEntity> walletEntities = walletAccountDAO.findAll();
    if (walletEntities == null || walletEntities.isEmpty()) {
      return Collections.emptySet();
    } else {
      return walletEntities.stream().map(this::fromEntity).collect(Collectors.toSet());
    }
  }

  public long getWalletsCount() {
    return walletAccountDAO.count();
  }

  public Wallet getWalletByIdentityId(long identityId) {
    WalletEntity walletEntity = walletAccountDAO.find(identityId);
    if (walletEntity == null) {
      return null;
    }
    return fromEntity(walletEntity);
  }

  public Wallet getWalletByAddress(String address) {
    WalletEntity walletEntity = walletAccountDAO.findByAddress(address.toLowerCase());
    if (walletEntity == null) {
      return null;
    }
    return fromEntity(walletEntity);
  }

  public void saveWallet(Wallet wallet, boolean isNew) {
    WalletEntity walletEntity = toEntity(wallet);

    if (isNew) {
      walletAccountDAO.create(walletEntity);
    } else {
      walletAccountDAO.update(walletEntity);
    }
  }

  public Wallet removeWallet(long identityId) {
    WalletEntity walletEntity = walletAccountDAO.find(identityId);
    walletAccountDAO.delete(walletEntity);
    return fromEntity(walletEntity);
  }

  private Wallet fromEntity(WalletEntity walletEntity) {
    Wallet wallet = new Wallet();
    wallet.setTechnicalId(walletEntity.getId());
    wallet.setEnabled(walletEntity.isEnabled());
    wallet.setAddress(walletEntity.getAddress());
    wallet.setPassPhrase(walletEntity.getPassPhrase());

    Identity identity = getIdentityById(walletEntity.getId());
    if (wallet.isEnabled()) {
      wallet.setEnabled(identity.isEnable() && !identity.isDeleted());
    }

    WalletType type = walletEntity.getType();
    wallet.setType(type.getId());
    if (type.isUser()) {
      wallet.setName(identity.getProfile().getFullName());
    } else {
      Space space = getSpace(identity.getRemoteId());
      wallet.setName(space.getDisplayName());
      wallet.setSpaceId(Long.parseLong(space.getId()));
    }

    wallet.setId(identity.getRemoteId());
    wallet.setAvatar(LinkProvider.buildAvatarURL(identity.getProviderId(), identity.getRemoteId()));
    return wallet;
  }

  private WalletEntity toEntity(Wallet wallet) {
    WalletEntity walletEntity = new WalletEntity();
    walletEntity.setId(wallet.getTechnicalId());
    walletEntity.setAddress(wallet.getAddress().toLowerCase());
    walletEntity.setEnabled(wallet.isEnabled());
    walletEntity.setPassPhrase(wallet.getPassPhrase());
    walletEntity.setType(WalletType.getType(wallet.getType()));
    return walletEntity;
  }

}
