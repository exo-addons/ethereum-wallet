package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.Wallet;
import org.exoplatform.addon.ethereum.wallet.model.WalletType;
import org.exoplatform.addon.ethereum.wallet.storage.AccountStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;

public class EthereumWalletAccountService {

  private static final Log LOG =
                               ExoLogger.getLogger(EthereumWalletAccountService.class);

  private AccountStorage   accountStorage;

  private ListenerService  listenerService;

  public EthereumWalletAccountService(AccountStorage walletAccountStorage) {
    this.accountStorage = walletAccountStorage;
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
   * Retrieve wallet details by identity type and remoteId
   * 
   * @param type
   * @param remoteId
   * @return {@link Wallet}
   */
  public Wallet getWalletByTypeAndID(String type, String remoteId) {
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

    Wallet wallet = accountStorage.getWalletByIdentityId(Long.parseLong(identity.getId()));
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
   * @param modifierUsername
   * @throws Exception
   */
  public void saveWallet(Wallet wallet, String modifierUsername, boolean broadcast) throws Exception {
    if (wallet == null) {
      throw new IllegalArgumentException("Wallet is mandatory");
    }

    if (StringUtils.isBlank(wallet.getAddress())) {
      throw new IllegalArgumentException("Wallet address is empty, thus it can't be saved");
    }

    computeWalletIdentity(wallet);

    Wallet oldWallet = accountStorage.getWalletByIdentityId(wallet.getTechnicalId());
    checkCanSaveWallet(wallet, oldWallet, modifierUsername);

    boolean isNew = oldWallet == null;
    wallet.setEnabled(isNew || wallet.isEnabled());

    setWalletPassPhrase(wallet, oldWallet, isNew);

    accountStorage.saveWallet(wallet, isNew);

    if (broadcast) {
      getListenerService().broadcast(isNew ? NEW_ADDRESS_ASSOCIATED_EVENT : MODIFY_ADDRESS_ASSOCIATED_EVENT, oldWallet, wallet);
    }
  }

  /**
   * Remove User or Space wallet address association
   * 
   * @param address
   * @param username
   * @throws IllegalAccessException
   */
  public void removeWalletByAddress(String address, String username) throws IllegalAccessException {
    if (address == null) {
      throw new IllegalArgumentException("address paramter is mandatory");
    }
    Wallet wallet = accountStorage.getWalletByAddress(address);
    if (wallet == null) {
      throw new IllegalStateException("Can't find wallet associated to address " + address);
    }
    if (!isUserAdmin(username)) {
      throw new IllegalAccessException("Current user " + username + " attempts to delete wallet with address " + address + " of "
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
        LOG.error("User '{}' attempts to modify wallet address of user '{}'", modifierUsername, remoteId);
        throw new IllegalAccessException();
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

  private void setWalletPassPhrase(Wallet wallet, Wallet oldWallet, boolean isNew) {
    if (isNew) {
      wallet.setPassPhrase(generateSecurityPhrase());
    } else {
      wallet.setPassPhrase(oldWallet.getPassPhrase());

      if (StringUtils.isBlank(wallet.getPassPhrase())) {
        LOG.warn("Wallet of type {} and id {} hasn't a generated passPhrase while it's not new, generating a new one",
                 wallet.getType(),
                 wallet.getId());
        wallet.setPassPhrase(generateSecurityPhrase());
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
