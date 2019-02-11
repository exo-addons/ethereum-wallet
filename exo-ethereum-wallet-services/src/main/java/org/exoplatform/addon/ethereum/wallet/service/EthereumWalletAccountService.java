package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.storage.AccountStorage;
import org.exoplatform.addon.ethereum.wallet.storage.AddressLabelStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;

public class EthereumWalletAccountService {

  private static final Log    LOG =
                                  ExoLogger.getLogger(EthereumWalletAccountService.class);

  private AccountStorage      accountStorage;

  private AddressLabelStorage labelStorage;

  private ListenerService     listenerService;

  public EthereumWalletAccountService(AccountStorage walletAccountStorage, AddressLabelStorage labelStorage) {
    this.accountStorage = walletAccountStorage;
    this.labelStorage = labelStorage;
  }

  /**
   * Retrieves the list registered wallets
   * 
   * @return
   */
  public Set<Wallet> listWallets() {
    Set<Wallet> wallets = accountStorage.listWallets();
    wallets.forEach(wallet -> wallet.setPassPhrase(null));
    return wallets;
  }

  /**
   * Retrieve wallets count
   * 
   * @return
   */
  public long getWalletsCount() {
    return accountStorage.getWalletsCount();
  }

  /**
   * Retrieve wallet details by identity technical id
   * 
   * @param identityId
   * @return {@link Wallet}
   */
  public Wallet getWalletByIdentityId(long identityId) {
    if (identityId == 0) {
      throw new IllegalArgumentException("identityId is mandatory");
    }
    Identity identity = getIdentityById(identityId);
    if (identity == null) {
      throw new IllegalArgumentException("Can't find identity with id " + identityId);
    }
    return getWalletOfIdentity(identity);
  }

  /**
   * Retrieve wallet details by identity type and remoteId accessed by a user
   * 
   * @param type
   * @param remoteId
   * @param currentUser
   * @return {@link Wallet}
   */
  public Wallet getWalletByTypeAndId(String type, String remoteId, String currentUser) {
    Wallet wallet = getWalletByTypeAndId(type, remoteId);
    if (wallet != null) {
      if (WalletType.isSpace(wallet.getType())) {
        wallet.setSpaceAdministrator(isUserSpaceManager(wallet.getId(), currentUser));
        if (!wallet.isSpaceAdministrator()) {
          // Delete passphrase for non managers
          wallet.setPassPhrase(null);
        }
      } else if (!StringUtils.equals(wallet.getId(), currentUser)) {
        // Delete passphrase for other users
        wallet.setPassPhrase(null);
      }
    }
    return wallet;
  }

  /**
   * Retrieve wallet details by identity type and remoteId
   * 
   * @param type
   * @param remoteId
   * @return {@link Wallet}
   */
  public Wallet getWalletByTypeAndId(String type, String remoteId) {
    if (StringUtils.isBlank(remoteId)) {
      throw new IllegalArgumentException("id parameter is mandatory");
    }
    WalletType accountType = WalletType.getType(type);
    if (accountType.isSpace()) {
      // Ensure to get a fresh prettyName of space
      remoteId = getSpacePrettyName(remoteId);
    }
    Identity identity = getIdentityByTypeAndId(accountType, remoteId);
    if (identity == null) {
      throw new IllegalArgumentException("Can't find identity with id " + remoteId + " and type " + accountType.getId());
    }

    return getWalletOfIdentity(identity);
  }

  /**
   * Retrieve wallet by address
   * 
   * @param address
   * @return {@link Wallet}
   */
  public Wallet getWalletByAddress(String address) {
    if (address == null) {
      throw new IllegalArgumentException("address is mandatory");
    }
    Wallet wallet = accountStorage.getWalletByAddress(address);
    if (wallet != null) {
      Identity identity = getIdentityById(wallet.getTechnicalId());
      wallet.setEnabled(wallet.isEnabled() && identity.isEnable() && !identity.isDeleted());
      wallet.setDisabledUser(!identity.isEnable());
      wallet.setDeletedUser(identity.isDeleted());
    }
    return wallet;
  }

  /**
   * Save wallet address to currentUser or to a space managed by current user
   * 
   * @param wallet
   * @param currentUser
   * @param broadcast
   * @throws Exception
   */
  public void saveWallet(Wallet wallet, String currentUser, boolean broadcast) throws Exception {
    if (wallet == null) {
      throw new IllegalArgumentException("Wallet is mandatory");
    }

    if (StringUtils.isBlank(wallet.getAddress())) {
      throw new IllegalArgumentException("Wallet address is empty, thus it can't be saved");
    }

    computeWalletIdentity(wallet);

    Wallet oldWallet = accountStorage.getWalletByIdentityId(wallet.getTechnicalId());
    checkCanSaveWallet(wallet, oldWallet, currentUser);

    boolean isNew = oldWallet == null;
    wallet.setEnabled(isNew || wallet.isEnabled());

    setWalletPassPhrase(wallet, oldWallet);

    accountStorage.saveWallet(wallet, isNew);

    if (broadcast) {
      getListenerService().broadcast(isNew ? NEW_ADDRESS_ASSOCIATED_EVENT : MODIFY_ADDRESS_ASSOCIATED_EVENT,
                                     oldWallet == null ? null : oldWallet.clone(),
                                     wallet.clone());
    }
  }

  /**
   * Remove User or Space wallet address association
   * 
   * @param address
   * @param currentUser
   * @throws IllegalAccessException
   */
  public void removeWalletByAddress(String address, String currentUser) throws IllegalAccessException {
    if (address == null) {
      throw new IllegalArgumentException("address paramter is mandatory");
    }
    Wallet wallet = accountStorage.getWalletByAddress(address);
    if (wallet == null) {
      throw new IllegalStateException("Can't find wallet associated to address " + address);
    }
    if (!isUserAdmin(currentUser)) {
      throw new IllegalAccessException("Current user " + currentUser + " attempts to delete wallet with address " + address + " of "
          + wallet.getType() + " " + wallet.getId());
    }
    accountStorage.removeWallet(wallet.getTechnicalId());
  }

  /**
   * Disable User or Space wallet
   * 
   * @param address
   * @param enable
   * @param username
   * @throws IllegalAccessException
   */
  public void enableWalletByAddress(String address, boolean enable, String username) throws IllegalAccessException {
    if (address == null) {
      throw new IllegalArgumentException("address paramter is mandatory");
    }
    Wallet wallet = accountStorage.getWalletByAddress(address);
    if (wallet == null) {
      throw new IllegalStateException("Can't find wallet associated to address " + address);
    }
    if (!isUserAdmin(username)) {
      throw new IllegalAccessException("User " + username + " attempts to disable wallet with address " + address + " of "
          + wallet.getType() + " " + wallet.getId());
    }
    wallet.setEnabled(enable);
    accountStorage.saveWallet(wallet, false);
  }

  /**
   * Return true if user can access wallet detailed information
   * 
   * @param wallet
   * @param username
   * @return
   */
  public boolean canAccessWallet(Wallet wallet, String username) {
    String remoteId = wallet.getId();
    WalletType type = WalletType.getType(wallet.getType());
    boolean isUserAdmin = isUserAdmin(username);

    if (isUserAdmin) {
      return true;
    }

    return (type.isUser() && StringUtils.equals(username, remoteId))
        || (type.isSpace() && isUserSpaceMember(wallet.getId(), username));
  }

  /**
   * Throws an exception if the user is not allowed to modify wallet information
   * 
   * @param wallet
   * @param oldWallet
   * @param modifierUsername
   * @throws IllegalAccessException
   */
  public void checkCanSaveWallet(Wallet wallet, Wallet oldWallet, String modifierUsername) throws IllegalAccessException {
    if (isUserAdmin(modifierUsername)) {
      return;
    }

    String remoteId = wallet.getId();
    WalletType type = WalletType.getType(wallet.getType());
    if (type.isUser()) {
      if (!StringUtils.equals(modifierUsername, remoteId)) {
        throw new IllegalAccessException("User '" + modifierUsername + "' attempts to modify wallet address of user '" + remoteId
            + "'");
      }

      // Check if wallet is enabled for current user and check if he's admin
      if (oldWallet != null && !oldWallet.isEnabled()) {
        LOG.error("User '{}' attempts to modify his wallet while it's disabled", modifierUsername);
        throw new IllegalAccessException();
      }
    } else {
      checkUserIsSpaceManager(remoteId, modifierUsername, true);
    }

    Wallet walletByAddress = accountStorage.getWalletByAddress(wallet.getAddress());
    if (walletByAddress != null && walletByAddress.getId() != wallet.getId()) {
      throw new IllegalStateException("User " + modifierUsername + " attempts to assign address of wallet of "
          + walletByAddress);
    }
  }

  public AddressLabel saveOrDeleteAddressLabel(AddressLabel label, String modifier) {
    if (label == null) {
      throw new IllegalArgumentException("Label is empty");
    }
    long labelId = label.getId();
    if (labelId > 0) {
      Identity identity = getIdentityByTypeAndId(WalletType.USER, modifier);
      if (identity == null) {
        throw new IllegalStateException("Can't find identity of user " + modifier);
      }
      AddressLabel storedLabel = labelStorage.getLabel(labelId);
      if (storedLabel == null) {
        label.setId(0);
      } else if (!StringUtils.equals(identity.getId(), String.valueOf(storedLabel.getIdentityId()))) {
        LOG.info("{} user modified address {} label from '{}' to '{}'",
                 modifier,
                 label.getAddress(),
                 storedLabel.getLabel(),
                 label.getLabel());
      }
    }

    if (StringUtils.isBlank(label.getLabel())) {
      if (labelId > 0) {
        labelStorage.removeLabel(label);
      }
    } else {
      label = labelStorage.saveLabel(label);
    }
    return label;
  }

  public Set<AddressLabel> getAddressesLabelsVisibleBy(String username) {
    if (!isUserAdmin(username)) {
      return Collections.emptySet();
    }
    return labelStorage.getAllLabels();
  }

  private Wallet getWalletOfIdentity(Identity identity) {
    long identityId = Long.parseLong(identity.getId());
    Wallet wallet = accountStorage.getWalletByIdentityId(identityId);
    if (wallet == null) {
      wallet = new Wallet();
      computeWalletFromIdentity(wallet, identity);
    } else {
      wallet.setEnabled(wallet.isEnabled() && identity.isEnable() && !identity.isDeleted());
    }
    wallet.setDisabledUser(!identity.isEnable());
    wallet.setDeletedUser(identity.isDeleted());
    return wallet;
  }

  private void setWalletPassPhrase(Wallet wallet, Wallet oldWallet) {
    if (StringUtils.isBlank(wallet.getPassPhrase())) {
      if (oldWallet == null || StringUtils.isBlank(oldWallet.getPassPhrase())) {
        wallet.setPassPhrase(generateSecurityPhrase());
      } else {
        wallet.setPassPhrase(oldWallet.getPassPhrase());
      }
    }
  }

  private String generateSecurityPhrase() {
    return RandomStringUtils.random(20, SIMPLE_CHARS);
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }

}
