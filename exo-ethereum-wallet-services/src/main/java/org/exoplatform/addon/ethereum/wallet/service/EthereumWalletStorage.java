/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.GLOAL_SETTINGS_CHANGED_EVENT;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * A storage service to save/load information used by users and spaces wallets
 */
public class EthereumWalletStorage {

  private static final Log    LOG                           = ExoLogger.getLogger(EthereumWalletStorage.class);

  public static final String  DEFAULT_NETWORK_ID            = "defaultNetworkId";

  public static final String  DEFAULT_NETWORK_URL           = "defaultNetworkURL";

  public static final String  DEFAULT_NETWORK_WS_URL        = "defaultNetworkWSURL";

  public static final String  DEFAULT_ACCESS_PERMISSION     = "defaultAccessPermission";

  public static final String  DEFAULT_GAS                   = "defaultGas";

  public static final String  DEFAULT_BLOCKS_TO_RETRIEVE    = "defaultBlocksToRetrieve";

  public static final String  DEFAULT_CONTRACTS_ADDRESSES   = "defaultContractAddresses";

  public static final String  SCOPE_NAME                    = "ADDONS_ETHEREUM_WALLET";

  public static final String  GLOBAL_SETTINGS_KEY_NAME      = "GLOBAL_SETTINGS";

  public static final String  ADDRESS_KEY_NAME              = "ADDONS_ETHEREUM_WALLET_ADDRESS";

  public static final String  LAST_BLOCK_NUMBER_KEY_NAME    = "ADDONS_ETHEREUM_LAST_BLOCK_NUMBER";

  public static final String  SETTINGS_KEY_NAME             = "ADDONS_ETHEREUM_WALLET_SETTINGS";

  public static final Context WALLET_CONTEXT                = Context.GLOBAL;

  public static final Scope   WALLET_SCOPE                  = Scope.APPLICATION.id(SCOPE_NAME);

  public static final String  WALLET_DEFAULT_CONTRACTS_NAME = "WALLET_DEFAULT_CONTRACTS";

  private SettingService      settingService;

  private IdentityManager     identityManager;

  private SpaceService        spaceService;

  private ListenerService     listenerService;

  private GlobalSettings      defaultSettings               = new GlobalSettings();

  private GlobalSettings      storedSettings;

  public EthereumWalletStorage(SettingService settingService,
                               SpaceService spaceService,
                               IdentityManager identityManager,
                               ListenerService listenerService,
                               InitParams params) {
    this.settingService = settingService;
    this.identityManager = identityManager;
    this.spaceService = spaceService;
    this.listenerService = listenerService;

    if (params.containsKey(DEFAULT_NETWORK_ID)) {
      String value = params.getValueParam(DEFAULT_NETWORK_ID).getValue();
      long defaultNetworkId = Long.parseLong(value);
      defaultSettings.setDefaultNetworkId(defaultNetworkId);
    }

    if (params.containsKey(DEFAULT_NETWORK_URL)) {
      String defaultNetworkURL = params.getValueParam(DEFAULT_NETWORK_URL).getValue();
      defaultSettings.setProviderURL(defaultNetworkURL);
    }

    if (params.containsKey(DEFAULT_ACCESS_PERMISSION)) {
      String defaultAccessPermission = params.getValueParam(DEFAULT_ACCESS_PERMISSION).getValue();
      defaultSettings.setAccessPermission(defaultAccessPermission);
    }

    if (params.containsKey(DEFAULT_GAS)) {
      String value = params.getValueParam(DEFAULT_GAS).getValue();
      int defaultGas = Integer.parseInt(value);
      defaultSettings.setDefaultGas(defaultGas);
    }

    if (params.containsKey(DEFAULT_BLOCKS_TO_RETRIEVE)) {
      String value = params.getValueParam(DEFAULT_BLOCKS_TO_RETRIEVE).getValue();
      int defaultBlocksToRetrieve = Integer.parseInt(value);
      defaultSettings.setDefaultBlocksToRetrieve(defaultBlocksToRetrieve);
    }

    if (params.containsKey(DEFAULT_CONTRACTS_ADDRESSES)) {
      String defaultContractsToDisplay = params.getValueParam(DEFAULT_CONTRACTS_ADDRESSES).getValue();
      if (StringUtils.isNotBlank(defaultContractsToDisplay)) {
        List<String> defaultContracts = Arrays.stream(defaultContractsToDisplay.split(","))
                                              .map(contractAddress -> contractAddress.trim().toLowerCase())
                                              .filter(contractAddress -> !contractAddress.isEmpty())
                                              .collect(Collectors.toList());
        defaultSettings.setDefaultContractsToDisplay(defaultContracts);
      }
    }
  }

  /**
   * Save global settings
   * 
   * @param globalSettings
   */
  public void saveSettings(GlobalSettings globalSettings) {
    if (globalSettings == null) {
      throw new IllegalArgumentException("globalSettings parameter is mandatory");
    }

    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       GLOBAL_SETTINGS_KEY_NAME,
                       SettingValue.create(globalSettings.toJSONString()));

    // Clear cached in memory stored settings
    this.storedSettings = null;

    try {
      this.listenerService.broadcast(GLOAL_SETTINGS_CHANGED_EVENT, this, globalSettings);
    } catch (Exception e) {
      LOG.error("An error occurred while broadcasting wallet settings modification event", e);
    }
  }

  /**
   * Retrieves global stored settings. if username is not null, the personal
   * settings will be included.
   * 
   * @param networkId
   * @param username
   * @return
   */
  public GlobalSettings getSettings(Long networkId, String username) {
    if (username != null && storedSettings != null) {
      // Retrieve stored global settings from memory
      return storedSettings;
    }

    SettingValue<?> globalSettingsValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, GLOBAL_SETTINGS_KEY_NAME);

    GlobalSettings globalSettings = defaultSettings;
    if (globalSettingsValue != null && globalSettingsValue.getValue() != null) {
      globalSettings = GlobalSettings.parseStringToObject(defaultSettings, globalSettingsValue.getValue().toString());
      if (StringUtils.isNotBlank(globalSettings.getAccessPermission())) {
        Space space = spaceService.getSpaceByPrettyName(globalSettings.getAccessPermission());
        if (space == null) {
          space = spaceService.getSpaceByUrl(globalSettings.getAccessPermission());
          if (space == null) {
            space = spaceService.getSpaceByGroupId("/spaces/" + globalSettings.getAccessPermission());
          }
        }
        // Disable wallet for users not member of the permitted space members
        if (username != null && space != null
            && !(spaceService.isMember(space, username) || spaceService.isSuperManager(username))) {
          globalSettings = new GlobalSettings();
          globalSettings.setWalletEnabled(false);
        }
      }
    }

    if (globalSettings.isWalletEnabled()) {
      if ((networkId == null || networkId == 0) && globalSettings.getDefaultNetworkId() != null) {
        networkId = globalSettings.getDefaultNetworkId();
      }
      // Retrieve default contracts to display for all users
      globalSettings.setDefaultContractsToDisplay(getDefaultContractsAddresses(networkId));

      if (username != null) {
        // Append user preferences
        SettingValue<?> userSettingsValue = settingService.get(Context.USER.id(username), WALLET_SCOPE, SETTINGS_KEY_NAME);
        UserPreferences userSettings = null;
        if (userSettingsValue != null && userSettingsValue.getValue() != null) {
          userSettings = UserPreferences.parseStringToObject(userSettingsValue.getValue().toString());
        }
        globalSettings.setUserDefaultGas(userSettings == null
            || userSettings.getDefaultGas() == 0 ? globalSettings.getDefaultGas() : userSettings.getDefaultGas());
      }
    }

    if (username == null) {
      storedSettings = globalSettings;
    }
    return globalSettings;
  }

  /**
   * Save a new contract address to display it in wallet of all users and save
   * contract name and symbol
   * 
   * @param contractDetail
   */
  public void saveDefaultContract(ContractDetail contractDetail) {
    if (StringUtils.isBlank(contractDetail.getAddress())) {
      throw new IllegalArgumentException("address parameter is mandatory");
    }
    if (contractDetail.getNetworkId() == null || contractDetail.getNetworkId() == 0) {
      throw new IllegalArgumentException("networkId parameter is mandatory");
    }

    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + contractDetail.getNetworkId();

    String address = contractDetail.getAddress().toLowerCase();

    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       address + contractDetail.getNetworkId(),
                       SettingValue.create(contractDetail.toJSONString()));

    // Save the contract address in the list of default contract addreses
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey);
    String defaultContractsAddresses =
                                     defaultContractsAddressesValue == null ? address
                                                                            : defaultContractsAddressesValue.getValue().toString()
                                                                                + "," + address;
    settingService.set(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey, SettingValue.create(defaultContractsAddresses));

    // Clear cached in memory stored settings
    this.storedSettings = null;
  }

  /**
   * Removes a contract address from default contracts displayed in wallet of
   * all users
   * 
   * @param address
   * @param networkId
   * @return
   */
  public boolean removeDefaultContract(String address, Long networkId) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Can't remove empty address for contract");
      return false;
    }
    if (networkId == null || networkId == 0) {
      LOG.warn("Can't remove empty network id for contract");
      return false;
    }

    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    final String defaultAddressToSave = address.toLowerCase();
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey);
    if (defaultContractsAddressesValue != null) {
      String[] contractAddresses = defaultContractsAddressesValue.getValue().toString().split(",");
      Set<String> contractAddressList = Arrays.stream(contractAddresses)
                                              .filter(contractAddress -> !contractAddress.equalsIgnoreCase(defaultAddressToSave))
                                              .collect(Collectors.toSet());
      String contractAddressValue = StringUtils.join(contractAddressList, ",");

      settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, address + networkId);
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey, SettingValue.create(contractAddressValue));
    }

    // Clear cached in memory stored settings
    this.storedSettings = null;

    return true;
  }

  /**
   * Get default contract detail
   * 
   * @param address
   * @param networkId
   * @return
   */
  public ContractDetail getDefaultContractDetail(String address, Long networkId) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Can't remove empty address for contract");
      return null;
    }
    if (networkId == null || networkId == 0) {
      LOG.warn("Can't remove empty network id for contract");
      return null;
    }

    SettingValue<?> contractDetailValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, address + networkId);
    if (contractDetailValue != null) {
      return ContractDetail.parseStringToObject((String) contractDetailValue.getValue());
    }
    return null;
  }

  /**
   * Retrieves the list of default contract addreses
   * 
   * @param networkId
   * @return
   */
  public List<String> getDefaultContractsAddresses(Long networkId) {
    if (networkId == null || networkId == 0) {
      return Collections.emptyList();
    }
    List<String> contractAddressList = null;
    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey);
    if (defaultContractsAddressesValue != null) {
      String[] contractAddresses = defaultContractsAddressesValue.getValue().toString().split(",");
      contractAddressList = Arrays.stream(contractAddresses).map(String::toLowerCase).collect(Collectors.toList());
    } else {
      contractAddressList = Collections.emptyList();
    }
    return contractAddressList;
  }

  /**
   * Save user preferences of Wallet
   * 
   * @param userPreferences
   */
  public void saveUserPreferences(String userId, UserPreferences userPreferences) {
    if (userPreferences == null) {
      throw new IllegalArgumentException("userPreferences parameter is mandatory");
    }
    settingService.set(Context.USER.id(userId),
                       WALLET_SCOPE,
                       SETTINGS_KEY_NAME,
                       SettingValue.create(userPreferences.toJSONString()));

    // Clear cached in memory stored settings
    this.storedSettings = null;
  }

  /**
   * Retrieve Space account details DTO
   * 
   * @param id
   * @return {@link AccountDetail}
   */
  public AccountDetail getSpaceDetails(String id) {
    if (id == null) {
      throw new IllegalArgumentException("id parameter is mandatory");
    }

    Space space = getSpace(id);
    if (space == null) {
      return null;
    }
    AccountDetail accountDetail = new AccountDetail(id,
                                                    "space",
                                                    space.getDisplayName(),
                                                    null,
                                                    space.getManagers(),
                                                    space.getAvatarUrl());
    return accountDetail;
  }

  /**
   * Retrieve User account details DTO
   * 
   * @param id
   * @return
   */
  public AccountDetail getUserDetails(String id) {
    if (id == null) {
      throw new IllegalArgumentException("id parameter is mandatory");
    }

    Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
    if (identity == null || identity.getProfile() == null) {
      return null;
    }

    String avatarUrl = identity.getProfile().getAvatarUrl();
    if (StringUtils.isBlank(avatarUrl)) {
      avatarUrl = "/rest/v1/social/users/" + id + "/avatar";
    }
    AccountDetail accountDetail = new AccountDetail(id, "user", identity.getProfile().getFullName(), null, null, avatarUrl);
    return accountDetail;
  }

  /**
   * Retrieve User or Space account details DTO by wallet address
   * 
   * @param address
   * @return
   */
  public AccountDetail getAccountDetailsByAddress(String address) {
    if (address == null) {
      throw new IllegalArgumentException("address parameter is mandatory");
    }

    AccountDetail accountDetail = null;

    SettingValue<?> walletAddressValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, address);
    if (walletAddressValue != null && walletAddressValue.getValue() != null) {
      String idAndType = walletAddressValue.getValue().toString();
      String id = null;
      if (idAndType.startsWith("user")) {
        id = idAndType.replaceFirst("user", "");
        accountDetail = getUserDetails(id);
      } else if (idAndType.startsWith("space")) {
        id = idAndType.replaceFirst("space", "");
        accountDetail = getSpaceDetails(id);
      }
      if (accountDetail == null) {
        LOG.info("Can't find the user/space with id {} associated to address {}", id, address);
      } else {
        accountDetail.setAddress(address);
      }
    }
    return accountDetail;
  }

  /**
   * Get associated address to a space
   * 
   * @param id
   * @return
   */
  public String getSpaceAddress(String id) {
    SettingValue<?> spaceWalletAddressValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, id);
    String address = null;
    if (spaceWalletAddressValue != null && spaceWalletAddressValue.getValue() != null) {
      address = spaceWalletAddressValue.getValue().toString().toLowerCase();
    }
    return address;
  }

  /**
   * Get associated address to a user
   * 
   * @param id
   * @return
   */
  public String getUserAddress(String id) {
    SettingValue<?> userWalletAddressValue = settingService.get(Context.USER.id(id), WALLET_SCOPE, ADDRESS_KEY_NAME);
    String address = null;
    if (userWalletAddressValue != null && userWalletAddressValue.getValue() != null) {
      address = userWalletAddressValue.getValue().toString().toLowerCase();
    }
    return address;
  }

  /**
   * Save wallet address to currentUser or to a space managed to current user,
   * switch details in accountDetail parameter
   * 
   * @param accountDetail
   * @param currentUserId
   * @throws IllegalAccessException
   */
  public void saveWalletAddress(AccountDetail accountDetail, String currentUserId) throws IllegalAccessException {
    String id = accountDetail.getId();
    String type = accountDetail.getType();
    String address = accountDetail.getAddress();
    address = address.toLowerCase();

    if (StringUtils.isBlank(id) || StringUtils.isBlank(type)
        || !(StringUtils.equals(type, "user") || StringUtils.equals(type, "space"))) {
      LOG.warn("Bad request sent to server with id '{}', type '{}' and address '{}'", id, type, address);
      throw new IllegalStateException();
    }

    if (StringUtils.equals(type, "user")) {
      if (!StringUtils.equals(currentUserId, id)) {
        LOG.error("User '{}' attempts to modify wallet address of user '{}'", currentUserId, id);
        throw new IllegalAccessException();
      }
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, address, SettingValue.create(type + id));
      String oldAddress = getUserAddress(id);
      if (oldAddress != null) {
        AccountDetail userDetailsByOldAddress = getAccountDetailsByAddress(oldAddress);
        if (userDetailsByOldAddress != null) {
          LOG.warn("The address {} was assigned to user {} and changed to user {}",
                   oldAddress,
                   userDetailsByOldAddress.getId(),
                   currentUserId);
          settingService.remove(Context.USER.id(userDetailsByOldAddress.getId()), WALLET_SCOPE, ADDRESS_KEY_NAME);
        }
        // Remove old address mapping
        settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, oldAddress);
      }
      settingService.set(Context.USER.id(id), WALLET_SCOPE, ADDRESS_KEY_NAME, SettingValue.create(address));
    } else if (StringUtils.equals(type, "space")) {
      Space space = getSpace(id);
      if (space == null) {
        LOG.warn("Space not found with id '{}'", id);
        throw new IllegalStateException();
      }
      if (!spaceService.isManager(space, currentUserId) && !spaceService.isSuperManager(currentUserId)) {
        LOG.error("User '{}' attempts to modify wallet address of space '{}'", currentUserId, space.getDisplayName());
        throw new IllegalAccessException();
      }
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, address, SettingValue.create(type + id));
      String oldAddress = getSpaceAddress(id);
      if (oldAddress != null) {
        // Remove old address mapping
        settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, oldAddress);
      }
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, id, SettingValue.create(address));
    }
  }

  /**
   * Returns last watched block
   * 
   * @param networkId
   * @return
   */
  public long getLastWatchedBlockNumber(long networkId) {
    SettingValue<?> lastBlockNumberValue =
                                         settingService.get(WALLET_CONTEXT, WALLET_SCOPE, LAST_BLOCK_NUMBER_KEY_NAME + networkId);
    if (lastBlockNumberValue != null && lastBlockNumberValue.getValue() != null) {
      return Long.valueOf(lastBlockNumberValue.getValue().toString());
    }
    return 0;
  }

  /**
   * Save last watched block
   * 
   * @param networkId
   * @param lastWatchedBlockNumber
   */
  public void saveLastWatchedBlockNumber(long networkId, long lastWatchedBlockNumber) {
    LOG.debug("Save watched block number {} on network {}", lastWatchedBlockNumber, networkId);
    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       LAST_BLOCK_NUMBER_KEY_NAME + networkId,
                       SettingValue.create(lastWatchedBlockNumber));
  }

  private Space getSpace(String id) {
    Space space = spaceService.getSpaceByPrettyName(id);
    if (space == null) {
      space = spaceService.getSpaceByUrl(id);
      if (space == null) {
        space = spaceService.getSpaceByDisplayName(id);
      }
    }
    if (space == null) {
      return null;
    }
    return space;
  }

}
