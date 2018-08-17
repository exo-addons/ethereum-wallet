package org.exoplatform.addon.ethereum.wallet.rest;

import static org.exoplatform.addon.ethereum.wallet.rest.Utils.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.model.UserPreferences;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * This class provide a REST endpoint to save/delete a contract as default
 * displayed contracts for end users
 */
@Path("/wallet/api/global-settings")
public class EthereumWalletGlobalSettingsREST implements ResourceContainer, Startable {

  private static final Log           LOG             = ExoLogger.getLogger(EthereumWalletGlobalSettingsREST.class);

  private ExoContainer               container;

  private SettingService             settingService;

  private EthereumWalletContractREST ethereumWalletContract;

  private SpaceService               spaceService;

  private GlobalSettings             defaultSettings = new GlobalSettings();

  public EthereumWalletGlobalSettingsREST(EthereumWalletContractREST ethereumWalletContract,
                                          SpaceService spaceService,
                                          SettingService settingService,
                                          ExoContainer container,
                                          InitParams params) {
    this.container = container;
    this.settingService = settingService;
    this.ethereumWalletContract = ethereumWalletContract;
    this.spaceService = spaceService;

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

  @Override
  public void start() {
    if (defaultSettings == null || defaultSettings.getDefaultContractsToDisplay() == null
        || defaultSettings.getDefaultContractsToDisplay().isEmpty()) {
      // No default contracts to store on DB
      return;
    }
    RequestLifeCycle.begin(this.container);
    try {
      SettingValue<?> settingValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, DEFAULT_CONTRACTS_ADDRESSES_STORED);
      if (settingValue != null && settingValue.getValue() != null) {
        // Default addresses has been stored in DB
        return;
      }
      List<String> contractAddresses = defaultSettings.getDefaultContractsToDisplay();
      for (String contractAddress : contractAddresses) {
        ethereumWalletContract.saveContract(contractAddress, defaultSettings.getDefaultNetworkId());
      }
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, DEFAULT_CONTRACTS_ADDRESSES_STORED, SettingValue.create("true"));
    } catch (Exception e) {
      LOG.error("Error starting EthereumWalletGlobalSettingsREST", e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public void stop() {
  }

  /**
   * Get global settings of aplication
   * 
   * @param networkId
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getSettings(@QueryParam("networkId") Long networkId) {
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
        // Disable wallet for users not member of the space
        if (space != null && !spaceService.isMember(space, getCurrentUserId())) {
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
      globalSettings.setDefaultContractsToDisplay(ethereumWalletContract.getContractAddresses(networkId));

      // Append user preferences
      SettingValue<?> userSettingsValue =
                                        settingService.get(Context.USER.id(getCurrentUserId()), WALLET_SCOPE, SETTINGS_KEY_NAME);
      UserPreferences userSettings = null;
      if (userSettingsValue != null && userSettingsValue.getValue() != null) {
        userSettings = UserPreferences.parseStringToObject(userSettingsValue.getValue().toString());
      }
      globalSettings.setUserDefaultGas(userSettings == null || userSettings.getDefaultGas() == 0 ? globalSettings.getDefaultGas()
                                                                                                 : userSettings.getDefaultGas());
    }

    return Response.ok(globalSettings.toJSONString()).build();
  }

  /**
   * Save global settings
   * 
   * @param globalSettings
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("save")
  @RolesAllowed("administrators")
  public Response saveSettings(GlobalSettings globalSettings) {
    if (globalSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(400).build();
    }
    if (StringUtils.isBlank(globalSettings.getProviderURL())) {
      LOG.warn("Bad request sent to server with empty setting 'providerURL'");
      return Response.status(400).build();
    }
    if (globalSettings.getDefaultBlocksToRetrieve() == null) {
      LOG.warn("Bad request sent to server with empty setting 'defaultBlocksToRetrieve'");
      return Response.status(400).build();
    }
    if (globalSettings.getDefaultNetworkId() == null) {
      LOG.warn("Bad request sent to server with empty setting 'defaultNetworkId'");
      return Response.status(400).build();
    }
    if (globalSettings.getDefaultGas() == null) {
      LOG.warn("Bad request sent to server with empty setting 'defaultGas'");
      return Response.status(400).build();
    }

    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       GLOBAL_SETTINGS_KEY_NAME,
                       SettingValue.create(globalSettings.toJSONString()));
    return Response.ok().build();
  }
}
