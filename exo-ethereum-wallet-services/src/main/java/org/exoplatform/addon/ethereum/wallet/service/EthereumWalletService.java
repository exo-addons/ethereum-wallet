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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.managed.EthereumWalletServiceManaged;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.storage.WebNotificationStorage;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.ManagedBy;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * A storage service to save/load information used by users and spaces wallets
 */
@ManagedBy(EthereumWalletServiceManaged.class)
public class EthereumWalletService implements Startable {

  private static final Log              LOG             =
                                            ExoLogger.getLogger(EthereumWalletService.class);

  private EthereumWalletContractService contractService;

  private EthereumWalletAccountService  accountService;

  private SettingService                settingService;

  private SpaceService                  spaceService;

  private WebNotificationStorage        webNotificationStorage;

  private ListenerService               listenerService;

  private GlobalSettings                defaultSettings =
                                                        new GlobalSettings();

  private GlobalSettings                storedSettings;

  public EthereumWalletService(EthereumWalletContractService contractService,
                               EthereumWalletAccountService accountService,
                               SettingService settingService,
                               SpaceService spaceService,
                               WebNotificationStorage webNotificationStorage,
                               InitParams params) {
    this.settingService = settingService;
    this.accountService = accountService;
    this.contractService = contractService;
    this.spaceService = spaceService;
    this.webNotificationStorage = webNotificationStorage;

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
  }

  @Override
  public void start() {
    // check global settings upgrade
    checkDataToUpgrade(getSettings());
  }

  @Override
  public void stop() {
    // Nothing to stop
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
    saveSettings(newGlobalSettings, oldGlobalSettings.getDataVersion());
  }

  public void saveSettings(GlobalSettings newGlobalSettings, Integer dataVersion) {
    if (newGlobalSettings == null) {
      throw new IllegalArgumentException("globalSettings parameter is mandatory");
    }

    GlobalSettings oldGlobalSettings = getSettings();

    newGlobalSettings.setDataVersion(dataVersion);

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
      getListenerService().broadcast(GLOAL_SETTINGS_CHANGED_EVENT, oldGlobalSettings, newGlobalSettings);
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
      retrieveContractsPreferences(this.storedSettings, this.storedSettings.getDefaultNetworkId());
      // Retrieve stored global settings from memory
      return this.storedSettings;
    } else {
      this.storedSettings = getSettings(null);
    }
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
    GlobalSettings globalSettings = null;
    if (this.storedSettings == null) {
      try {
        globalSettings = getSettings(networkId, null, null);
      } catch (IllegalAccessException e) {
        return null;
      }
    } else {
      globalSettings = this.storedSettings.clone();
    }
    if (globalSettings == null) {
      globalSettings = defaultSettings.clone();
    }
    if ((networkId == null || networkId == 0) && globalSettings.getDefaultNetworkId() != null) {
      networkId = globalSettings.getDefaultNetworkId();
    }
    retrieveContractsPreferences(globalSettings, networkId);
    return globalSettings;
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
   * @throws IllegalAccessException
   */
  public GlobalSettings getSettings(Long networkId, String spaceId, String username) throws IllegalAccessException {
    GlobalSettings globalSettings = null;
    if (StringUtils.isBlank(username)) {
      // Retrieve settings without computed user data
      return getStoredGlobalSettings();
    } else {
      globalSettings = getSettings(networkId);
    }

    globalSettings.setAdmin(isUserAdmin(username));
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
      WalletPreferences userSettings = null;
      if (userSettingsValue != null && userSettingsValue.getValue() != null) {
        userSettings = WalletPreferences.parseStringToObject(userSettingsValue.getValue().toString());
        checkDataToUpgrade(username, userSettings);
      } else {
        userSettings = new WalletPreferences();
      }
      globalSettings.setUserPreferences(userSettings);

      if (StringUtils.isNotBlank(spaceId)) {
        Wallet wallet = accountService.getWalletByTypeAndID(WalletType.SPACE.getId(), spaceId);
        if (wallet != null) {
          if (!accountService.canAccessWallet(wallet, username)) {
            throw new IllegalAccessException("User " + username + " is not allowed to display wallet of space " + spaceId);
          }
          userSettings.setPhrase(wallet.getPassPhrase());
          userSettings.setWalletAddress(wallet.getAddress());
          userSettings.setWallet(wallet);
        }
      } else {
        Wallet wallet = accountService.getWalletByTypeAndID(WalletType.USER.getId(), username);
        if (wallet != null) {
          userSettings.setPhrase(wallet.getPassPhrase());
          userSettings.setWalletAddress(wallet.getAddress());
          userSettings.setWallet(wallet);
        }
      }

      globalSettings.setContractAbi(contractService.getContractAbi());
      globalSettings.setContractBin(contractService.getContractBinary());
    }
    return globalSettings;
  }

  private void retrieveContractsPreferences(GlobalSettings globalSettings, Long networkId) {
    if ((networkId == null || networkId == 0) && globalSettings.getDefaultNetworkId() != null) {
      networkId = globalSettings.getDefaultNetworkId();
    }

    // Retrieve default contracts to display for all users
    globalSettings.setDefaultContractsToDisplay(contractService.getDefaultContractsAddresses(networkId));

    // Generic global settings computing
    String defaultPrincipalAccount = globalSettings.getDefaultPrincipalAccount();
    if (StringUtils.isNotBlank(defaultPrincipalAccount)) {
      ContractDetail principalContractDetails = contractService.getContractDetail(defaultPrincipalAccount, networkId);
      globalSettings.setPrincipalContractAdminAddress(principalContractDetails == null ? null
                                                                                       : principalContractDetails.getOwner());
    }
  }

  /**
   * Save user preferences of Wallet
   * 
   * @param userPreferences
   */
  public void saveUserPreferences(String userId, WalletPreferences userPreferences) {
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
   * Request funds
   * 
   * @param fundsRequest
   * @throws IllegalAccessException
   */
  public void requestFunds(FundsRequest fundsRequest) throws IllegalAccessException {
    String currentUser = getCurrentUserId();

    Wallet requestSender = accountService.getWalletByAddress(fundsRequest.getAddress());
    if (requestSender == null) {
      throw new IllegalStateException("Bad request sent to server with unknown sender address");
    }

    String requestSenderId = requestSender.getId();
    String requestSenderType = requestSender.getType();

    if (WalletType.isUser(requestSenderType) && !StringUtils.equals(currentUser, requestSenderId)) {
      LOG.warn("Bad request sent to server with invalid sender type or id {} / {}", requestSenderType, requestSenderId);
      throw new IllegalAccessException("Bad request sent to server with invalid sender");
    }

    if (WalletType.isSpace(requestSenderType) && !isUserSpaceMember(fundsRequest.getReceipient(), requestSenderId)) {
      throw new IllegalAccessException("Request sender is not allowed to request funds from space");
    }

    NotificationContext ctx = NotificationContextImpl.cloneInstance();

    GlobalSettings settings = getSettings();
    if (!StringUtils.isBlank(fundsRequest.getContract())) {
      ContractDetail contractDetail =
                                    contractService.getContractDetail(fundsRequest.getContract(), settings.getDefaultNetworkId());
      if (contractDetail == null) {
        throw new IllegalStateException("Bad request sent to server with invalid contract address (O ly default addresses are permitted)");
      }
      ctx.append(CONTRACT_DETAILS_PARAMETER, contractDetail);
    }

    String requestReceipientId = fundsRequest.getReceipient();
    String requestReceipientType = fundsRequest.getReceipientType();

    Wallet requestReceipient = accountService.getWalletByTypeAndID(WalletType.getType(requestReceipientType).getId(),
                                                                   requestReceipientId);

    if (requestReceipient == null || requestReceipient.getTechnicalId() == 0) {
      LOG.warn("Can't find fund request recipient with id {} and type {}", requestReceipientId, requestReceipientType);
    }

    ctx.append(FUNDS_REQUEST_SENDER_DETAIL_PARAMETER,
               accountService.getWalletByTypeAndID(WalletType.USER.getId(), getCurrentUserId()));
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

  private GlobalSettings getStoredGlobalSettings() {
    GlobalSettings globalSettings = null;
    // Global settings computing
    SettingValue<?> globalSettingsValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, GLOBAL_SETTINGS_KEY_NAME);
    if (globalSettingsValue != null && globalSettingsValue.getValue() != null) {
      globalSettings = GlobalSettings.parseStringToObject(defaultSettings, globalSettingsValue.getValue().toString());
    }
    return globalSettings;
  }

  private void checkDataToUpgrade(String username, WalletPreferences userPreferences) {
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

        saveSettings(globalSettings, GLOBAL_DATA_VERSION);
        LOG.info("Global preferences has been upgraded to version {}", GLOBAL_DATA_VERSION);
      }
    } catch (Exception e) {
      LOG.warn("Can't upgrade global settings", e);
    }
  }

  public ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }
}
