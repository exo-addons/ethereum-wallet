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

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.picocontainer.Startable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.storage.WebNotificationStorage;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.*;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.ManagedBy;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * A storage service to save/load information used by users and spaces wallets
 */
@ManagedBy(EthereumWalletServiceManaged.class)
public class EthereumWalletService implements Startable {

  private static final String                                                                SPACE_WITH_ID_MESSAGE                  =
                                                                                                                   "Space with id '";

  private static final String                                                                IS_NOT_FOUND_MESSAGE                   =
                                                                                                                  "' is not found";

  private static final String                                                                ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE =
                                                                                                                                    "address parameter is mandatory";

  private static final Log                                                                   LOG                                    =
                                                                                                 ExoLogger.getLogger(EthereumWalletService.class);

  private ExoContainer                                                                       container;

  private EthereumClientConnector                                                            ethereumClientConnector;

  private SettingService                                                                     settingService;

  private IdentityManager                                                                    identityManager;

  private SpaceService                                                                       spaceService;

  private UserACL                                                                            userACL;

  private WebNotificationStorage                                                             webNotificationStorage;

  private ListenerService                                                                    listenerService;

  private ConfigurationManager                                                               configurationManager;

  private GlobalSettings                                                                     defaultSettings                        =
                                                                                                             new GlobalSettings();

  private GlobalSettings                                                                     storedSettings;

  private String                                                                             contractAbiPath;

  private JSONArray                                                                          contractAbi;

  private String                                                                             contractBinaryPath;

  private String                                                                             contractBinary;

  private ExoCache<String, TransactionDetail>                                                transactionDetailsCache                =
                                                                                                                     null;

  // Added as cache instead of Set<AccountDetail> for future usage in clustered
  // environments
  private ExoCache<AccountDetailCacheId, AccountDetail>                                      accountDetailCache                     =
                                                                                                                null;

  /*
   * Key: Address or id of entity (space group id prefix or username). Value:
   * account details. Context: Type of entity or address if null.
   */
  private FutureExoCache<AccountDetailCacheId, AccountDetail, ServiceContext<AccountDetail>> accountDetailFutureCache               =
                                                                                                                      null;

  private long                                                                               walletsCount                           =
                                                                                                          0;

  private ScheduledExecutorService                                                           scheduledExecutorService               =
                                                                                                                      null;

  private int                                                                                knownTreatedTransactionsCount          =
                                                                                                                           0;

  public EthereumWalletService(SettingService settingService,
                               SpaceService spaceService,
                               WebNotificationStorage webNotificationStorage,
                               IdentityManager identityManager,
                               ListenerService listenerService,
                               UserACL userACL,
                               CacheService cacheService,
                               ExoContainer exoContainer,
                               ConfigurationManager configurationManager,
                               InitParams params) {
    this.container = exoContainer;
    this.configurationManager = configurationManager;
    this.settingService = settingService;
    this.identityManager = identityManager;
    this.spaceService = spaceService;
    this.webNotificationStorage = webNotificationStorage;
    this.listenerService = listenerService;
    this.userACL = userACL;
    this.transactionDetailsCache = cacheService.getCacheInstance("wallet.transactionsMessages");
    this.accountDetailCache = cacheService.getCacheInstance("wallet.accountDetailCache");

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
      long defaultGas = Long.parseLong(value);
      defaultSettings.setDefaultGas(defaultGas);
    }

    if (params.containsKey(MIN_GAS_PRICE)) {
      String value = params.getValueParam(MIN_GAS_PRICE).getValue();
      long minGasPrice = Long.parseLong(value);
      defaultSettings.setMinGasPrice(minGasPrice);
    }

    if (params.containsKey(NORMAL_GAS_PRICE)) {
      String value = params.getValueParam(NORMAL_GAS_PRICE).getValue();
      long normalGasPrice = Long.parseLong(value);
      defaultSettings.setNormalGasPrice(normalGasPrice);
    }

    if (params.containsKey(MAX_GAS_PRICE)) {
      String value = params.getValueParam(MAX_GAS_PRICE).getValue();
      long maxGasPrice = Long.parseLong(value);
      defaultSettings.setMaxGasPrice(maxGasPrice);
    }

    if (params.containsKey(DEFAULT_CONTRACTS_ADDRESSES)) {
      String defaultContractsToDisplay = params.getValueParam(DEFAULT_CONTRACTS_ADDRESSES).getValue();
      if (StringUtils.isNotBlank(defaultContractsToDisplay)) {
        Set<String> defaultContracts = Arrays.stream(defaultContractsToDisplay.split(","))
                                             .map(contractAddress -> contractAddress.trim().toLowerCase())
                                             .filter(contractAddress -> !contractAddress.isEmpty())
                                             .collect(Collectors.toSet());
        defaultSettings.setDefaultContractsToDisplay(defaultContracts);
      }
    }

    if (params.containsKey(ABI_PATH_PARAMETER)) {
      contractAbiPath = params.getValueParam(ABI_PATH_PARAMETER).getValue();
    }
    if (StringUtils.isBlank(contractAbiPath)) {
      LOG.warn("Contract ABI path is empty, thus no contract deployment is possible");
    }
    if (params.containsKey(BIN_PATH_PARAMETER)) {
      contractBinaryPath = params.getValueParam(BIN_PATH_PARAMETER).getValue();
    }
    if (StringUtils.isBlank(contractBinaryPath)) {
      LOG.warn("Contract BIN path is empty, thus no contract deployment is possible");
    }
    this.accountDetailFutureCache =
                                  new FutureExoCache<>(new Loader<AccountDetailCacheId, AccountDetail, ServiceContext<AccountDetail>>() {
                                    @Override
                                    public AccountDetail retrieve(ServiceContext<AccountDetail> context,
                                                                  AccountDetailCacheId key) throws Exception {
                                      return context.execute();
                                    }
                                  }, accountDetailCache);
  }

  @Override
  public void start() {
    try {
      String contractAbiString = IOUtil.getStreamContentAsString(this.configurationManager.getInputStream(contractAbiPath));
      contractAbi = new JSONArray(contractAbiString);
      contractBinary = IOUtil.getStreamContentAsString(this.configurationManager.getInputStream(contractBinaryPath));
      if (!contractBinary.startsWith("0x")) {
        contractBinary = "0x" + contractBinary;
      }
    } catch (Exception e) {
      LOG.error("Can't read ABI/BIN files content", e);
    }

    // check global settings upgrade
    checkDataToUpgrade(getSettings());

    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-cache-populator-%d").build();
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
    // Transactions Queue processing
    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(this.container);
      try {
        this.walletsCount = 0;
        this.listWallets();
      } catch (Exception e) {
        LOG.error("Error while retrieving list of wallets for cache population", e);
      } finally {
        RequestLifeCycle.end();
      }
    }, 10, accountDetailCache.getLiveTime() > 0 ? accountDetailCache.getLiveTime() : 86400, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
  }

  /**
   * Get Contract ABI
   * 
   * @return
   */
  public JSONArray getContractAbi() {
    return contractAbi;
  }

  /**
   * Get Contract BINARY to deploy
   * 
   * @return
   */
  public String getContractBinary() {
    return contractBinary;
  }

  /**
   * Save global settings
   * 
   * @param newGlobalSettings
   */
  public void saveSettings(GlobalSettings newGlobalSettings) {
    if (newGlobalSettings == null) {
      throw new IllegalArgumentException("globalSettings parameter is mandatory");
    }

    GlobalSettings oldGlobalSettings = getSettings();

    // Set not updatable data from UI
    newGlobalSettings.setDataVersion(oldGlobalSettings.getDataVersion());

    // Delete computed data
    newGlobalSettings.setUserPreferences(null);
    newGlobalSettings.setContractAbi(null);
    newGlobalSettings.setContractBin(null);
    newGlobalSettings.setWalletEnabled(false);
    newGlobalSettings.setAdmin(false);

    LOG.debug("Saving new global settings", newGlobalSettings.toJSONString(false));

    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       GLOBAL_SETTINGS_KEY_NAME,
                       SettingValue.create(newGlobalSettings.toJSONString(false)));

    // Clear cached in memory stored settings
    this.storedSettings = null;

    try {
      this.listenerService.broadcast(GLOAL_SETTINGS_CHANGED_EVENT, oldGlobalSettings, newGlobalSettings);
    } catch (Exception e) {
      LOG.error("An error occurred while broadcasting wallet settings modification event", e);
    }
  }

  /**
   * Retrieves global stored settings used for all users.
   * 
   * @return
   */
  public GlobalSettings getSettings() {
    if (this.storedSettings != null) {
      // Retrieve stored global settings from memory
      return this.storedSettings;
    }
    this.storedSettings = getSettings(null);
    return this.storedSettings.clone();
  }

  /**
   * Retrieves global stored settings. if username is not null, the personal
   * settings will be included.
   * 
   * @param networkId
   * @return
   */
  public GlobalSettings getSettings(Long networkId) {
    GlobalSettings globalStoredSettings = this.storedSettings;
    if (globalStoredSettings != null && globalStoredSettings.getDefaultNetworkId() != null
        && (networkId == null || globalStoredSettings.getDefaultNetworkId() == networkId)) {
      return globalStoredSettings.clone();
    }
    return getSettings(networkId, null, null);
  }

  /**
   * Retrieves global stored settings. if username is not null, the personal
   * settings will be included. if spaceId is not null wallet address will be
   * retrieved
   * 
   * @param networkId
   * @param spaceId
   * @param username
   * @return
   */
  public GlobalSettings getSettings(Long networkId, String spaceId, String username) {
    GlobalSettings globalSettings = null;
    if (StringUtils.isNotBlank(username)) {
      globalSettings = getSettings(networkId);
    } else {
      // Global settings computing
      SettingValue<?> globalSettingsValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, GLOBAL_SETTINGS_KEY_NAME);
      if (globalSettingsValue != null && globalSettingsValue.getValue() != null) {
        globalSettings = GlobalSettings.parseStringToObject(defaultSettings, globalSettingsValue.getValue().toString());
      }

      if (globalSettings == null) {
        globalSettings = defaultSettings.clone();
      }

      if ((networkId == null || networkId == 0) && globalSettings.getDefaultNetworkId() != null) {
        networkId = globalSettings.getDefaultNetworkId();
      }

      // Retrieve default contracts to display for all users
      globalSettings.setDefaultContractsToDisplay(getDefaultContractsAddresses(networkId));

      // Generic global settings computing
      String defaultPrincipalAccount = globalSettings.getDefaultPrincipalAccount();
      if (StringUtils.isNotBlank(defaultPrincipalAccount)) {
        ContractDetail principalContractDetails = getContractDetail(defaultPrincipalAccount,
                                                                    globalSettings.getDefaultNetworkId());
        globalSettings.setPrincipalContractAdminAddress(principalContractDetails == null ? null
                                                                                         : principalContractDetails.getOwner());
      }
      return globalSettings;
    }

    globalSettings.setAdmin(isUserAdmin());
    globalSettings.setWalletEnabled(true);

    if (StringUtils.isNotBlank(globalSettings.getAccessPermission())) {
      Space space = getSpace(globalSettings.getAccessPermission());
      // Disable wallet for users not member of the permitted space members
      if (space != null && !(spaceService.isMember(space, username) || spaceService.isSuperManager(username))) {
        LOG.info("Wallet is disabled for user {} because he's not member of space {}", username, space.getPrettyName());
        globalSettings.setWalletEnabled(false);
      }
    }

    if (globalSettings.isWalletEnabled() || globalSettings.isAdmin()) {
      // Append user preferences
      SettingValue<?> userSettingsValue = settingService.get(Context.USER.id(username), WALLET_SCOPE, SETTINGS_KEY_NAME);
      UserPreferences userSettings = null;
      if (userSettingsValue != null && userSettingsValue.getValue() != null) {
        userSettings = UserPreferences.parseStringToObject(userSettingsValue.getValue().toString());
        checkDataToUpgrade(username, userSettings);
      } else {
        userSettings = new UserPreferences();
      }
      globalSettings.setUserPreferences(userSettings);

      if (StringUtils.isNotBlank(spaceId)) {
        String spacePhrase = getSpacePhrase(spaceId);
        if (StringUtils.isBlank(spacePhrase)) {
          Space space = getSpace(spaceId);
          if (space == null) {
            throw new IllegalArgumentException(SPACE_WITH_ID_MESSAGE + spaceId + IS_NOT_FOUND_MESSAGE);
          }
          spaceId = getSpaceId(space);
          spacePhrase = getSpacePhrase(spaceId);
        }
        userSettings.setPhrase(spacePhrase);
        userSettings.setWalletAddress(getSpaceAddress(spaceId));
      } else {
        userSettings.setWalletAddress(getUserAddress(username));
        userSettings.setPhrase(getUserPhrase(username));
      }

      globalSettings.setContractAbi(getContractAbi());
      globalSettings.setContractBin(getContractBinary());
    }
    return globalSettings;
  }

  /**
   * Save a new contract details
   * 
   * @param contractDetail
   */
  public void saveContract(ContractDetail contractDetail) {
    if (StringUtils.isBlank(contractDetail.getAddress())) {
      throw new IllegalArgumentException(ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE);
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

    if (contractDetail.isDefaultContract()) {
      // Save the contract address in the list of default contract addreses
      SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey);
      String defaultContractsAddresses = defaultContractsAddressesValue == null ? address
                                                                                : defaultContractsAddressesValue.getValue()
                                                                                                                .toString()
                                                                                    + "," + address;
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey, SettingValue.create(defaultContractsAddresses));

      // Clear cached in memory stored settings
      this.storedSettings = null;
    }
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
   * Get contract detail
   * 
   * @param address
   * @param networkId
   * @return
   */
  public ContractDetail getContractDetail(String address, Long networkId) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Can't get default contract detail with empty address");
      return null;
    }

    if (networkId == null || networkId == 0) {
      GlobalSettings settings = getSettings();
      networkId = settings == null ? 0 : settings.getDefaultNetworkId();
    }

    Set<String> defaultContracts = getDefaultContractsAddresses(networkId);
    if (defaultContracts != null && !defaultContracts.contains(address)) {
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
  public Set<String> getDefaultContractsAddresses(Long networkId) {
    if (networkId == null || networkId == 0) {
      return Collections.emptySet();
    }

    if (this.storedSettings != null && networkId == this.storedSettings.getDefaultNetworkId()
        && this.storedSettings.getDefaultContractsToDisplay() != null) {
      return this.storedSettings.getDefaultContractsToDisplay();
    }

    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey);
    if (defaultContractsAddressesValue != null) {
      String defaultContractsAddressesString = defaultContractsAddressesValue.getValue().toString().toLowerCase();
      String[] contractAddresses = defaultContractsAddressesString.split(",");
      return Arrays.stream(contractAddresses).map(String::toLowerCase).collect(Collectors.toSet());
    }
    return Collections.emptySet();
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
      throw new IllegalArgumentException(SPACE_WITH_ID_MESSAGE + id + IS_NOT_FOUND_MESSAGE);
    }
    id = getSpaceId(space);

    AccountDetail accountDetails = getAccountDetailsFromCache(new AccountDetailCacheId(SPACE_ACCOUNT_TYPE, id));
    if (accountDetails != null && StringUtils.isNotBlank(accountDetails.getAddress())) {
      putInCache(accountDetails);
    }
    return accountDetails;
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
    AccountDetail accountDetails = getAccountDetailsFromCache(new AccountDetailCacheId(USER_ACCOUNT_TYPE, id));
    if (accountDetails != null && StringUtils.isNotBlank(accountDetails.getAddress())) {
      putInCache(accountDetails);
    }
    return accountDetails;
  }

  /**
   * Retrieve User or Space account details DTO by wallet address
   * 
   * @param address
   * @return
   */
  public AccountDetail getAccountDetailsByAddress(String address) {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE);
    }
    AccountDetail accountDetails = getAccountDetailsFromCache(new AccountDetailCacheId(address.toLowerCase()));
    if (accountDetails != null && StringUtils.isNotBlank(accountDetails.getAddress())) {
      putInCache(accountDetails);
    }
    return accountDetails;
  }

  /**
   * Get associated address to a space
   * 
   * @param id
   * @return
   */
  public String getSpaceAddress(String id) {
    Space space = getSpace(id);
    if (space == null) {
      throw new IllegalArgumentException(SPACE_WITH_ID_MESSAGE + id + IS_NOT_FOUND_MESSAGE);
    }
    id = getSpaceId(space);

    AccountDetail accountDetail = getAccountDetailsFromCache(new AccountDetailCacheId(SPACE_ACCOUNT_TYPE, id));
    if (accountDetail != null) {
      return accountDetail.getAddress();
    }

    return getSpaceAddressFromStorage(id);
  }

  /**
   * Get associated address to a user
   * 
   * @param id
   * @return
   */
  public String getUserAddress(String id) {
    AccountDetail accountDetail = getAccountDetailsFromCache(new AccountDetailCacheId(USER_ACCOUNT_TYPE, id));
    if (accountDetail != null) {
      return accountDetail.getAddress();
    }

    return getUserAddressFromStorage(id);
  }

  /**
   * Save wallet address to currentUser or to a space managed to current user,
   * switch details in accountDetail parameter
   * 
   * @param accountDetail
   * @return
   * @throws IllegalAccessException
   */
  public String saveWalletAddress(AccountDetail accountDetail) throws Exception {
    String currentUserId = getCurrentUserId();
    String id = accountDetail.getId();
    String type = accountDetail.getType();
    if (StringUtils.equals(type, SPACE_ACCOUNT_TYPE)) {
      Space space = getSpace(id);
      if (space == null) {
        throw new IllegalArgumentException(SPACE_WITH_ID_MESSAGE + id + IS_NOT_FOUND_MESSAGE);
      }
      id = getSpaceId(space);
    }
    String address = accountDetail.getAddress();
    address = address.toLowerCase();

    removeFromCache(address);

    if (StringUtils.isBlank(id) || StringUtils.isBlank(type)
        || !(StringUtils.equals(type, USER_ACCOUNT_TYPE) || StringUtils.equals(type, SPACE_ACCOUNT_TYPE))) {
      LOG.warn("Bad request sent to server with id '{}', type '{}' and address '{}'", id, type, address);
      throw new IllegalStateException();
    }

    String oldAddress = null;
    if (StringUtils.equals(type, USER_ACCOUNT_TYPE)) {
      if (!StringUtils.equals(currentUserId, id)) {
        LOG.error("User '{}' attempts to modify wallet address of user '{}'", currentUserId, id);
        throw new IllegalAccessException();
      }

      oldAddress = getUserAddress(id);
      if (oldAddress != null && !StringUtils.equals(oldAddress, address)) {
        AccountDetail userDetailsByOldAddress = getAccountDetailsByAddress(oldAddress);
        if (userDetailsByOldAddress != null) {
          LOG.info("The address {} was assigned to user {} and changed to user {}",
                   oldAddress,
                   userDetailsByOldAddress.getId(),
                   currentUserId);
          settingService.remove(Context.USER.id(userDetailsByOldAddress.getId()), WALLET_SCOPE, ADDRESS_KEY_NAME);
        }
        // Remove old address mapping
        settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, oldAddress);
      }

      if (StringUtils.isNotBlank(oldAddress)) {
        removeFromCache(oldAddress);
      }

      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, address, SettingValue.create(type + id));
      settingService.set(Context.USER.id(id), WALLET_SCOPE, ADDRESS_KEY_NAME, SettingValue.create(address));
    } else if (StringUtils.equals(type, SPACE_ACCOUNT_TYPE)) {
      checkCurrentUserIsSpaceManager(id);
      oldAddress = getSpaceAddress(id);
      if (oldAddress != null && !StringUtils.equals(oldAddress, address)) {
        // Remove old address mapping
        settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, oldAddress);
      }

      if (StringUtils.isNotBlank(oldAddress)) {
        removeFromCache(oldAddress);
      }

      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, address, SettingValue.create(type + id));
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, id, SettingValue.create(address));
    } else {
      LOG.warn("Account type {} is not recognized", type);
      return null;
    }
    removeFromCache(type, id);

    if (StringUtils.isBlank(oldAddress)) {
      this.listenerService.broadcast(NEW_ADDRESS_ASSOCIATED_EVENT, this, accountDetail);
    } else {
      this.listenerService.broadcast(MODIFY_ADDRESS_ASSOCIATED_EVENT, this, accountDetail);
    }

    // Populate cache to keep all items into it to ease the wallets listing in
    // administration UI
    getAccountDetailsFromCache(new AccountDetailCacheId(accountDetail.getAddress()));
    getAccountDetailsFromCache(new AccountDetailCacheId(accountDetail.getType(), accountDetail.getId()));

    if (StringUtils.isBlank(oldAddress)) {
      this.walletsCount += 1;
    }
    return getOrGenerateSecurityPhrase(accountDetail);
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

  /**
   * Save transaction hash for an account
   * 
   * @param networkId
   * @param address
   * @param hash
   * @param sender
   * @param status
   * @throws Exception
   */
  public void saveAccountTransaction(Long networkId,
                                     String address,
                                     String hash,
                                     boolean sender,
                                     Boolean status) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE);
    }
    if (StringUtils.isBlank(hash)) {
      throw new IllegalArgumentException("transaction hash parameter is mandatory");
    }

    broadcastNewTransactionEvent(hash, address, status);

    address = address.toLowerCase();
    String addressTransactionsParamName = WALLET_USER_TRANSACTION_NAME + address + networkId;

    SettingValue<?> addressTransactionsValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, addressTransactionsParamName);
    String addressTransactions = addressTransactionsValue == null ? "" : addressTransactionsValue.getValue().toString();
    if (!addressTransactions.contains(hash)) {
      String content = hash;
      TransactionDetail transactionMessage = getTransactionDetailFromCache(hash);
      if (transactionMessage != null) {
        content = transactionMessage.getToStoreValue(sender);
      }
      addressTransactions = addressTransactions.isEmpty() ? content : content + "," + addressTransactions;
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, addressTransactionsParamName, SettingValue.create(addressTransactions));
    }
  }

  public void broadcastNewTransactionEvent(String hash, String address, Boolean status) throws Exception {
    if (status != null) {
      JSONObject transaction = new JSONObject();
      transaction.put("hash", hash);
      transaction.put("address", address);
      transaction.put("status", status);
      listenerService.broadcast(KNOWN_TRANSACTION_MINED_EVENT, null, transaction);
      this.knownTreatedTransactionsCount++;
    }
  }

  /**
   * Get list of transaction hashes per user
   * 
   * @param networkId
   * @param address
   * @param isAdministration
   * @return
   */
  public List<JSONObject> getAccountTransactions(Long networkId, String address, boolean isAdministration) {
    String addressTransactionsParamName = WALLET_USER_TRANSACTION_NAME + address + networkId;
    SettingValue<?> addressTransactionsValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, addressTransactionsParamName);
    String addressTransactions = addressTransactionsValue == null ? "" : addressTransactionsValue.getValue().toString();
    String[] addressTransactionsArray = addressTransactions.isEmpty() ? new String[0] : addressTransactions.split(",");
    String currentUserId = getCurrentUserId();

    AccountDetail accountToDisplayDetails = getAccountDetailsByAddress(address);
    // Avoid displaying labels in administration and when user is not admin of
    // space
    final boolean displayLabel = !isAdministration && displayTransactionsLabel(accountToDisplayDetails, currentUserId);

    return Arrays.stream(addressTransactionsArray).map(transaction -> {
      TransactionDetail transactionDetail = TransactionDetail.fromStoredValue(transaction);
      TransactionDetail cachedTransactionDetail = getTransactionDetailFromCache(transactionDetail.getHash());
      if (cachedTransactionDetail != null) {
        transactionDetail = cachedTransactionDetail;
      }

      if (!displayLabel) {
        transactionDetail.setLabel(null);
      }
      return transactionDetail.toJSONObject();
    }).collect(Collectors.toList());
  }

  /**
   * Request funds
   * 
   * @param fundsRequest
   * @throws IllegalAccessException
   */
  public void requestFunds(FundsRequest fundsRequest) throws IllegalAccessException {
    String currentUser = getCurrentUserId();

    AccountDetail requestSender = getAccountDetailsByAddress(fundsRequest.getAddress());
    if (requestSender == null) {
      throw new IllegalStateException("Bad request sent to server with unknown sender address");
    }

    String requestSenderId = requestSender.getId();
    String requestSenderType = requestSender.getType();

    if (StringUtils.equals(requestSenderType, USER_ACCOUNT_TYPE) && !StringUtils.equals(currentUser, requestSenderId)) {
      LOG.warn("Bad request sent to server with invalid sender type or id {} / {}", requestSenderType, requestSenderId);
      throw new IllegalAccessException("Bad request sent to server with invalid sender");
    }

    if (StringUtils.equals(requestSenderType, SPACE_ACCOUNT_TYPE)) {
      checkCurrentUserIsSpaceManager(requestSenderId);
    }

    NotificationContext ctx = NotificationContextImpl.cloneInstance();

    GlobalSettings settings = getSettings();
    if (!StringUtils.isBlank(fundsRequest.getContract())) {
      ContractDetail contractDetail = getContractDetail(fundsRequest.getContract(), settings.getDefaultNetworkId());
      if (contractDetail == null) {
        throw new IllegalStateException("Bad request sent to server with invalid contract address (O ly default addresses are permitted)");
      }
      ctx.append(CONTRACT_DETAILS_PARAMETER, contractDetail);
    }

    String requestReceipientId = fundsRequest.getReceipient();
    String requestReceipientType = fundsRequest.getReceipientType();

    AccountDetail requestReceipient = null;
    if (USER_ACCOUNT_TYPE.equals(requestReceipientType)) {
      requestReceipient = getUserDetails(requestReceipientId);
    } else if (SPACE_ACCOUNT_TYPE.equals(requestReceipientType)) {
      requestReceipient = getSpaceDetails(requestReceipientId);
    }

    if (requestReceipient == null || requestReceipient.getTechnicalId() == null) {
      LOG.warn("Can't find fund request recipient with id {} and type {}", requestReceipientId, requestReceipientType);
    }

    ctx.append(FUNDS_REQUEST_SENDER_DETAIL_PARAMETER, getUserDetails(getCurrentUserId()));
    ctx.append(SENDER_ACCOUNT_DETAIL_PARAMETER, requestSender);
    ctx.append(RECEIVER_ACCOUNT_DETAIL_PARAMETER, requestReceipient);
    ctx.append(FUNDS_REQUEST_PARAMETER, fundsRequest);

    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(FUNDS_REQUEST_NOTIFICATION_ID))).execute(ctx);
  }

  /**
   * Mark a fund request web notification as sent
   * 
   * @param notificationId
   * @param currentUser
   * @throws IllegalAccessException if current user is not the targetted user of
   *           notification
   */
  public void markFundRequestAsSent(String notificationId, String currentUser) throws IllegalAccessException {
    NotificationInfo notificationInfo = webNotificationStorage.get(notificationId);
    if (notificationInfo == null) {
      throw new IllegalStateException("Notification with id " + notificationId + " wasn't found");
    }
    if (notificationInfo.getTo() == null || !currentUser.equals(notificationInfo.getTo())) {
      throw new IllegalAccessException("Target user of notification '" + notificationId + "' is different from current user");
    }
    notificationInfo.getOwnerParameter().put(FUNDS_REQUEST_SENT, "true");
    webNotificationStorage.update(notificationInfo, false);
  }

  /**
   * Get fund request status
   * 
   * @param notificationId
   * @param currentUser
   * @return true if fund request sent
   * @throws IllegalAccessException if current user is not the targetted user of
   *           notification
   */
  public boolean isFundRequestSent(String notificationId, String currentUser) throws IllegalAccessException {
    NotificationInfo notificationInfo = webNotificationStorage.get(notificationId);
    if (notificationInfo == null) {
      throw new IllegalStateException("Notification with id " + notificationId + " wasn't found");
    }
    if (notificationInfo.getTo() == null || !currentUser.equals(notificationInfo.getTo())) {
      throw new IllegalAccessException("Target user of notification '" + notificationId + "' is different from current user");
    }
    String fundRequestSentString = notificationInfo.getOwnerParameter().get(FUNDS_REQUEST_SENT);
    return Boolean.parseBoolean(fundRequestSentString);
  }

  /**
   * Retrieves the list registered wallets
   * 
   * @return
   * @throws Exception
   */
  public Set<AccountDetail> listWallets() throws Exception {
    int cacheSize = this.accountDetailCache.getCacheSize();
    if (cacheSize > 0 && cacheSize >= this.walletsCount) {
      List<? extends AccountDetail> cachedObjects = this.accountDetailCache.getCachedObjects();
      // Using HashSet to remove duplicated objects
      return new HashSet<>(cachedObjects);
    }

    Set<AccountDetail> wallets = new HashSet<>();
    Map<String, String> usernames = getListOfWalletsOfType(USER_ACCOUNT_TYPE);
    for (Entry<String, String> user : usernames.entrySet()) {
      AccountDetail details = getUserDetails(user.getKey());
      if (details != null) {
        details.setAddress(user.getValue());
        Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, user.getKey(), true);
        if (identity == null || identity.getProfile() == null) {
          continue;
        }
        details.setEnabled(identity.isEnable() && !identity.isDeleted());
        wallets.add(details);
      }
    }
    this.walletsCount = this.accountDetailCache.getCacheSize() / 2;
    LOG.info("{} user wallets has been loaded", this.walletsCount);

    Map<String, String> spaces = getListOfWalletsOfType(SPACE_ACCOUNT_TYPE);
    for (Entry<String, String> space : spaces.entrySet()) {
      AccountDetail details = getSpaceDetails(space.getKey());
      if (details != null) {
        details.setAddress(space.getValue());
        wallets.add(details);
      }
    }
    long spaceWalletsCount = this.accountDetailCache.getCacheSize() / 2 - this.walletsCount;
    LOG.info("{} space wallets has been loaded", spaceWalletsCount);

    this.walletsCount = this.accountDetailCache.getCacheSize() / 2;
    LOG.info("{} total wallets has been loaded", this.walletsCount);

    return wallets;
  }

  /**
   * Save temporary transaction label and message and save transaction hash in
   * sender and receiver account
   *
   * @param transactionMessage
   * @throws Exception
   */
  public void saveTransactionDetail(TransactionDetail transactionMessage) throws Exception {
    GlobalSettings settings = getSettings();
    if (settings != null && (transactionMessage.getNetworkId() == null || transactionMessage.getNetworkId() == 0)) {
      transactionMessage.setNetworkId(settings.getDefaultNetworkId());
    }
    setTransactionDetailInCache(transactionMessage);

    AccountDetail senderAccount = null;
    if (StringUtils.isNotBlank(transactionMessage.getFrom())) {
      senderAccount = getAccountDetailsByAddress(transactionMessage.getFrom());
      if (senderAccount != null) {
        this.saveAccountTransaction(transactionMessage.getNetworkId(),
                                    transactionMessage.getFrom(),
                                    transactionMessage.getHash(),
                                    true,
                                    null);
      }
    }

    // Avoid notifying receiver for admin transaction until it's mined
    if (!transactionMessage.isAdminOperation() && StringUtils.isNotBlank(transactionMessage.getTo())
        && getAccountDetailsByAddress(transactionMessage.getTo()) != null) {
      this.saveAccountTransaction(transactionMessage.getNetworkId(),
                                  transactionMessage.getTo(),
                                  transactionMessage.getHash(),
                                  false,
                                  null);
    }

    String contractAddress = transactionMessage.getContractAddress();
    ContractDetail contractDetail = null;
    if (StringUtils.isBlank(contractAddress)) {
      contractDetail = getContractDetail(transactionMessage.getTo(), transactionMessage.getNetworkId());
      if (contractDetail != null) {
        contractAddress = transactionMessage.getTo();
      }
    }

    // Get contract address from "to" field
    if (StringUtils.isNotBlank(contractAddress)) {
      if (contractDetail == null) {
        contractDetail = getContractDetail(contractAddress, transactionMessage.getNetworkId());
      }
      if (contractDetail != null) {
        // Save message label when the sender is not a known user or when this
        // is an administration operation
        boolean saveLabelToContractTransactionsList = transactionMessage.isAdminOperation() || senderAccount == null;
        this.saveAccountTransaction(transactionMessage.getNetworkId(),
                                    contractAddress,
                                    transactionMessage.getHash(),
                                    saveLabelToContractTransactionsList,
                                    null);
      }
    }
  }

  public boolean isTransactionWatched(String hash) {
    // Is watched if the connector has started watching latest block
    // or the transaction is in cache
    return getEthereumClientConnector().getLastWatchedBlockNumber() <= getEthereumClientConnector().getLastBlockNumberOnStartupTime()
        || transactionDetailsCache.get(hash.toLowerCase()) != null;
  }

  /**
   * Set transaction in cache
   * 
   * @param transactionMessage
   */
  public void setTransactionDetailInCache(TransactionDetail transactionMessage) {
    this.transactionDetailsCache.put(transactionMessage.getHash().toLowerCase(), transactionMessage);
  }

  /**
   * Get temporary stored transaction details from cache
   * 
   * @param transactionHash
   * @return
   */
  public TransactionDetail getTransactionDetailFromCache(String transactionHash) {
    TransactionDetail transactionDetail = this.transactionDetailsCache.get(transactionHash.toLowerCase());
    return transactionDetail == null ? null : transactionDetail.copy();
  }

  /**
   * Remove transaction message object
   * 
   * @param hash
   * @return
   */
  public TransactionDetail removeTransactionDetailFromCache(String hash) {
    return this.transactionDetailsCache.remove(hash.toLowerCase());
  }

  /**
   * Retreive the ABI content of a contract
   * 
   * @param name
   * @return
   * @throws IOException
   */
  public String getContract(String name, String extension) throws IOException {
    try (InputStream abiInputStream = this.getClass()
                                          .getClassLoader()
                                          .getResourceAsStream("org/exoplatform/addon/ethereum/wallet/contract/" + name + "."
                                              + extension)) {
      return IOUtils.toString(abiInputStream);
    }
  }

  /**
   * @return true if user is member of /platform/users
   */
  public boolean isUserAdmin() {
    return userACL.isUserInGroup(ADMINISTRATORS_GROUP);
  }

  /**
   * Remove from cache the account detail of an address
   * 
   * @param address
   */
  public void removeFromCache(String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Enmpty cache key address");
      return;
    }
    AccountDetailCacheId addressKey = new AccountDetailCacheId(address);
    AccountDetail accountDetail = accountDetailCache.get(addressKey);
    if (accountDetail != null) {
      accountDetailFutureCache.remove(addressKey);
      accountDetailFutureCache.remove(new AccountDetailCacheId(accountDetail.getType(), accountDetail.getId()));
    }
  }

  /**
   * Removes from cache an account detail identified by type and id
   * 
   * @param type
   * @param id
   */
  public void removeFromCache(String type, String id) {
    if (StringUtils.isBlank(type) || StringUtils.isBlank(id)) {
      LOG.warn("Enmpty cache key: {}/{}", type, id);
      return;
    }
    AccountDetailCacheId typeIdKey = new AccountDetailCacheId(type, id);
    AccountDetail accountDetail = accountDetailCache.get(typeIdKey);
    if (accountDetail != null) {
      accountDetailFutureCache.remove(typeIdKey);
      String address = accountDetail.getAddress();
      if (StringUtils.isNotBlank(address)) {
        accountDetailFutureCache.remove(new AccountDetailCacheId(address));
      }
    }
  }

  public int getKnownTreatedTransactionsCount() {
    return knownTreatedTransactionsCount;
  }

  public long getWalletsCount() {
    return this.walletsCount;
  }

  private Map<String, String> getListOfWalletsOfType(String walletType) throws Exception {
    if (StringUtils.isBlank(walletType) || !(USER_ACCOUNT_TYPE.equals(walletType) || SPACE_ACCOUNT_TYPE.equals(walletType))) {
      throw new IllegalArgumentException("Unrecognized wallet type: " + walletType);
    }
    Map<String, String> names = new HashMap<>();
    if (USER_ACCOUNT_TYPE.equals(walletType)) {
      int pageSize = 100;
      int current = 0;
      List<Context> contexts = null;
      do {
        contexts = settingService.getContextsByTypeAndScopeAndSettingName(Context.USER.getName(),
                                                                          WALLET_SCOPE.getName(),
                                                                          WALLET_SCOPE.getId(),
                                                                          ADDRESS_KEY_NAME,
                                                                          current,
                                                                          pageSize);
        if (contexts != null && !contexts.isEmpty()) {
          List<String> usernames = contexts.stream().map(context -> context.getId()).collect(Collectors.toList());
          for (String username : usernames) {
            names.put(username, getUserAddress(username));
          }
        }
        current += pageSize;
      } while (contexts != null && contexts.size() == pageSize);
    } else {
      int pageSize = 100;
      int current = 0;
      Space[] spaces = null;
      do {
        ListAccess<Space> spacesListAccress = spaceService.getAllSpacesWithListAccess();
        spaces = spacesListAccress.load(current, pageSize);
        if (spaces != null && spaces.length > 0) {
          for (Space space : spaces) {
            SettingValue<?> spaceAddress = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, space.getPrettyName());
            if (spaceAddress != null && spaceAddress.getValue() != null) {
              names.put(space.getPrettyName(), spaceAddress.getValue().toString());
            } else if (!StringUtils.equals(space.getPrettyName(), getSpaceId(space))) {
              String spaceId = getSpaceId(space);
              spaceAddress = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, spaceId);
              if (spaceAddress != null && spaceAddress.getValue() != null) {
                names.put(spaceId, spaceAddress.getValue().toString());
              }
            }
          }
        }
        current += pageSize;
      } while (spaces != null && spaces.length == pageSize);
    }
    return names;
  }

  private void checkDataToUpgrade(String username, UserPreferences userPreferences) {
    try {
      int userDataVersion = userPreferences.getDataVersion() == null ? 0 : userPreferences.getDataVersion();
      if (userDataVersion < USER_DATA_VERSION) {

        // Upgrade default gas for new contract to upgrade
        if (userPreferences.getDataVersion() < DEFAULT_GAS_UPGRADE_VERSION) {
          userPreferences.setDefaultGas(defaultSettings.getDefaultGas());
        }

        userPreferences.setDataVersion(USER_DATA_VERSION);
        saveUserPreferences(username, userPreferences);
        LOG.info("User {} preferences has been upgraded to version {}", username, USER_DATA_VERSION);
      }
    } catch (Exception e) {
      LOG.warn("Can't upgrade data of user preferences: " + username, e);
    }
  }

  private void checkDataToUpgrade(GlobalSettings globalSettings) {
    try {
      int globalDataVersion = globalSettings.getDataVersion() == null ? 0 : globalSettings.getDataVersion();
      if (globalDataVersion < GLOBAL_DATA_VERSION) {

        // Upgrade default gas for new contract to upgrade
        if (globalSettings.getDataVersion() < DEFAULT_GAS_UPGRADE_VERSION) {
          globalSettings.setDefaultGas(defaultSettings.getDefaultGas());
        }

        // Upgrade default gas price to avoid excessive gas price on Main Net
        if (globalSettings.getDataVersion() < DEFAULT_GAS_PRICE_UPGRADE_VERSION) {
          globalSettings.setMinGasPrice(defaultSettings.getMinGasPrice());
          globalSettings.setNormalGasPrice(defaultSettings.getNormalGasPrice());
          globalSettings.setMaxGasPrice(defaultSettings.getMaxGasPrice());
        }

        globalSettings.setDataVersion(GLOBAL_DATA_VERSION);
        saveSettings(globalSettings);
        LOG.info("Global preferences has been upgraded to version {}", GLOBAL_DATA_VERSION);
      }
    } catch (Exception e) {
      LOG.warn("Can't upgrade global settings", e);
    }
  }

  private String getOrGenerateSecurityPhrase(AccountDetail accountDetail) throws IllegalAccessException {
    String currentUser = getCurrentUserId();
    String id = accountDetail.getId();
    String type = accountDetail.getType();

    Context context = null;
    String paramName = null;

    if (StringUtils.equals(type, USER_ACCOUNT_TYPE) && StringUtils.equals(currentUser, id)) {
      context = Context.USER.id(id);
      paramName = WALLET_BROWSER_PHRASE_NAME;
    } else if (StringUtils.equals(type, SPACE_ACCOUNT_TYPE)) {
      checkCurrentUserIsSpaceManager(id);
      context = WALLET_CONTEXT;
      paramName = WALLET_BROWSER_PHRASE_NAME + id;
    } else {
      return null;
    }

    SettingValue<?> browserWalletPhraseValue = settingService.get(context, WALLET_SCOPE, paramName);
    if (browserWalletPhraseValue != null && browserWalletPhraseValue.getValue() != null) {
      return browserWalletPhraseValue.getValue().toString();
    }

    String phrase = RandomStringUtils.random(20, SIMPLE_CHARS);
    settingService.set(context, WALLET_SCOPE, paramName, SettingValue.create(phrase));
    return phrase;
  }

  private String getUserPhrase(String username) {
    SettingValue<?> browserWalletPhraseValue = settingService.get(Context.USER.id(username),
                                                                  WALLET_SCOPE,
                                                                  WALLET_BROWSER_PHRASE_NAME);
    if (browserWalletPhraseValue != null && browserWalletPhraseValue.getValue() != null) {
      return browserWalletPhraseValue.getValue().toString();
    }
    return null;
  }

  private String getSpacePhrase(String spaceId) {
    try {
      boolean isSpaceManager = checkCurrentUserIsSpaceManager(spaceId, false);
      if (!isSpaceManager) {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
    SettingValue<?> browserWalletPhraseValue = settingService.get(WALLET_CONTEXT,
                                                                  WALLET_SCOPE,
                                                                  WALLET_BROWSER_PHRASE_NAME + spaceId);
    if (browserWalletPhraseValue != null && browserWalletPhraseValue.getValue() != null) {
      return browserWalletPhraseValue.getValue().toString();
    }
    return null;
  }

  private boolean checkCurrentUserIsSpaceManager(String id) throws IllegalAccessException {
    return checkCurrentUserIsSpaceManager(id, true);
  }

  private boolean checkCurrentUserIsSpaceManager(String spaceId, boolean throwException) throws IllegalAccessException {
    String currentUserId = getCurrentUserId();
    Space space = getSpace(spaceId);
    if (space == null) {
      LOG.warn("Space not found with id '{}'", spaceId);
      throw new IllegalStateException();
    }
    if (!spaceService.isManager(space, currentUserId) && !spaceService.isSuperManager(currentUserId)) {
      if (throwException) {
        LOG.error("User '{}' attempts to modify wallet address of space '{}'", currentUserId, space.getDisplayName());
        throw new IllegalAccessException();
      } else {
        return false;
      }
    }
    return true;
  }

  private AccountDetail getAccountDetailsFromCache(AccountDetailCacheId accountDetailCacheId) {
    if (accountDetailCacheId == null
        || (StringUtils.isBlank(accountDetailCacheId.getAddress()) && StringUtils.isBlank(accountDetailCacheId.getId()))) {
      LOG.warn("cache key is mandatory");
      return null;
    }
    String currentUserId = getCurrentUserId();
    AccountDetail accountDetails = accountDetailFutureCache.get(new ServiceContext<AccountDetail>() {
      @Override
      public AccountDetail execute() {
        AccountDetail accountDetail = null;
        if (StringUtils.isNotBlank(accountDetailCacheId.getAddress())) {
          String address = accountDetailCacheId.getAddress();
          // Search by address
          SettingValue<?> walletAddressValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, address);
          if (walletAddressValue != null && walletAddressValue.getValue() != null) {
            String idAndType = walletAddressValue.getValue().toString();
            String id = null;
            if (idAndType.startsWith(USER_ACCOUNT_TYPE)) {
              id = idAndType.replaceFirst(USER_ACCOUNT_TYPE, "");
              accountDetail = getUserDetails(id);
            } else if (idAndType.startsWith(SPACE_ACCOUNT_TYPE)) {
              id = idAndType.replaceFirst(SPACE_ACCOUNT_TYPE, "");
              accountDetail = getSpaceDetails(id);
            }
            if (accountDetail == null) {
              LOG.info("Can't find the user/space with id {} associated to address {}", id, address);
            } else {
              accountDetail.setAddress(address);
            }
          }
        } else if (USER_ACCOUNT_TYPE.equals(accountDetailCacheId.getType())) {
          String id = accountDetailCacheId.getId();
          Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
          if (identity == null || identity.getProfile() == null) {
            return null;
          }

          String avatarUrl = identity.getProfile().getAvatarUrl();
          if (StringUtils.isBlank(avatarUrl)) {
            avatarUrl = "/rest/v1/social/users/" + id + "/avatar";
          }
          return new AccountDetail(id,
                                   identity.getId(),
                                   USER_ACCOUNT_TYPE,
                                   identity.getProfile().getFullName(),
                                   getUserAddressFromStorage(id),
                                   false,
                                   identity.isEnable(),
                                   avatarUrl);
        } else if (SPACE_ACCOUNT_TYPE.equals(accountDetailCacheId.getType())) {
          String id = accountDetailCacheId.getId();
          Space space = getSpace(id);
          if (space == null) {
            return null;
          }
          id = getSpaceId(space);

          String avatarUrl = space.getAvatarUrl();
          String prettyName = space.getPrettyName();
          String oldPrettyName = space.getGroupId().substring(SpaceUtils.SPACE_GROUP.length() + 1);
          if (StringUtils.isBlank(avatarUrl)) {
            avatarUrl = "/rest/v1/social/spaces/" + prettyName + "/avatar";
          }
          String spaceAddress = getSpaceAddressFromStorage(id);
          AccountDetail spaceAccountDetail = new AccountDetail(id,
                                                               space.getId(),
                                                               SPACE_ACCOUNT_TYPE,
                                                               space.getDisplayName(),
                                                               spaceAddress,
                                                               spaceService.isManager(space, currentUserId)
                                                                   || spaceService.isSuperManager(currentUserId),
                                                               true,
                                                               avatarUrl);
          if (StringUtils.isBlank(spaceAccountDetail.getAddress()) && !StringUtils.equals(id, prettyName)) {
            // Detected renamed space
            fixRenamedSpaceAssociation(spaceAccountDetail, id, prettyName);
          }
          if (StringUtils.isBlank(spaceAccountDetail.getAddress()) && !StringUtils.equals(id, oldPrettyName)
              && !StringUtils.equals(prettyName, oldPrettyName)) {
            // Detected renamed space
            fixRenamedSpaceAssociation(spaceAccountDetail, id, oldPrettyName);
          }
          return spaceAccountDetail;
        }
        return accountDetail;
      }
    }, accountDetailCacheId);

    // don't keep in cache only users with addresses
    if (accountDetails != null && StringUtils.isBlank(accountDetails.getAddress())) {
      removeFromCache(accountDetails.getType(), accountDetails.getId());
    }
    if (accountDetails != null && SPACE_ACCOUNT_TYPE.equals(accountDetails.getType())) {
      Space space = getSpace(accountDetails.getId());
      if (space == null) {
        accountDetails.setSpaceAdministrator(false);
      } else {
        accountDetails.setSpaceAdministrator(spaceService.isManager(space, currentUserId)
            || spaceService.isSuperManager(currentUserId));
      }
    }
    return accountDetails;
  }

  private String getUserAddressFromStorage(String id) {
    SettingValue<?> userWalletAddressValue = settingService.get(Context.USER.id(id), WALLET_SCOPE, ADDRESS_KEY_NAME);
    if (userWalletAddressValue != null && userWalletAddressValue.getValue() != null) {
      return userWalletAddressValue.getValue().toString().toLowerCase();
    }
    return null;
  }

  private String getSpaceAddressFromStorage(String id) {
    SettingValue<?> spaceWalletAddressValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, id);
    if (spaceWalletAddressValue != null && spaceWalletAddressValue.getValue() != null) {
      return spaceWalletAddressValue.getValue().toString().toLowerCase();
    }
    return null;
  }

  private void putInCache(AccountDetail details) {
    if (details != null) {
      accountDetailCache.putLocal(new AccountDetailCacheId(details.getAddress()), details);
      accountDetailCache.putLocal(new AccountDetailCacheId(details.getType(), details.getId()), details);
    }
  }

  private void fixRenamedSpaceAssociation(AccountDetail spaceAccountDetail, String prettyName, String oldPrettyName) {
    try {
      String spaceAddress = getSpaceAddressFromStorage(prettyName);
      if (StringUtils.isBlank(spaceAddress)) {
        spaceAddress = getSpaceAddressFromStorage(oldPrettyName);
        if (StringUtils.isNotBlank(spaceAddress)) {
          spaceAccountDetail.setAddress(spaceAddress);
          settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, oldPrettyName);
          settingService.set(WALLET_CONTEXT, WALLET_SCOPE, spaceAddress, SettingValue.create(SPACE_ACCOUNT_TYPE + prettyName));
          settingService.set(WALLET_CONTEXT, WALLET_SCOPE, prettyName, SettingValue.create(spaceAddress));
          removeFromCache(SPACE_ACCOUNT_TYPE, oldPrettyName);
        }
      }
    } catch (Exception e) {
      LOG.warn("Error when fixing space '" + prettyName + "' address association after renaming", e);
    }
  }

  private boolean displayTransactionsLabel(AccountDetail senderAccountDetails, String currentUserId) {
    if (senderAccountDetails == null) {
      return isUserAdmin();
    }
    String accountId = senderAccountDetails.getId();
    String accountType = senderAccountDetails.getType();
    if (StringUtils.isBlank(accountId) || StringUtils.isBlank(accountType)) {
      return isUserAdmin();
    }

    if (SPACE_ACCOUNT_TYPE.equals(accountType)) {
      if (spaceService.isSuperManager(currentUserId)) {
        return true;
      }
      Space space = getSpace(accountId);
      return space != null && spaceService.isManager(space, currentUserId);
    } else {
      return StringUtils.equalsIgnoreCase(accountId, currentUserId);
    }
  }

  private EthereumClientConnector getEthereumClientConnector() {
    if (ethereumClientConnector == null) {
      ethereumClientConnector = CommonsUtils.getService(EthereumClientConnector.class);
    }
    return ethereumClientConnector;
  }

  public interface ServiceContext<V> {
    V execute();
  }

}
