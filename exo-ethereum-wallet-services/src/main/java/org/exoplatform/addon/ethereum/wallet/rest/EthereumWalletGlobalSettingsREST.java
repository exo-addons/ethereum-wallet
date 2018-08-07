package org.exoplatform.addon.ethereum.wallet.rest;

import static org.exoplatform.addon.ethereum.wallet.rest.Utils.*;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.model.UserPreferences;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/delete a contract as default
 * displayed contracts for end users
 */
@Path("/wallet/api/global-settings")
public class EthereumWalletGlobalSettingsREST implements ResourceContainer {

  public static final Log            LOG = ExoLogger.getLogger(EthereumWalletGlobalSettingsREST.class);

  private SettingService             settingService;

  private EthereumWalletContractREST ethereumWalletContract;

  public EthereumWalletGlobalSettingsREST(EthereumWalletContractREST ethereumWalletContract, SettingService settingService) {
    this.settingService = settingService;
    this.ethereumWalletContract = ethereumWalletContract;
  }

  /**
   * Get global settings of aplication
   * 
   * @return
   * @throws Exception
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getSettings() throws Exception {
    SettingValue<?> globalSettingsValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, GLOBAL_SETTINGS_KEY_NAME);

    GlobalSettings globalSettings = null;
    if (globalSettingsValue == null || globalSettingsValue.getValue() == null) {
      globalSettings = new GlobalSettings();

    } else {
      globalSettings = GlobalSettings.parseStringToObject(globalSettingsValue.getValue().toString());
    }
    globalSettings.setDefaultContractsToDisplay(ethereumWalletContract.getContractAddresses());

    // Append user preferences
    SettingValue<?> userSettingsValue = settingService.get(Context.USER.id(getCurrentUserId()), WALLET_SCOPE, SETTINGS_KEY_NAME);
    if (userSettingsValue != null && userSettingsValue.getValue() != null) {
      UserPreferences userSettings = UserPreferences.parseStringToObject(userSettingsValue.getValue().toString());
      globalSettings.setUserDefaultGas(userSettings.getDefaultGas());
    }

    return Response.ok(globalSettings.toJSONString()).build();
  }

  /**
   * Save global preferences
   * 
   * @param accessPermissions
   * @param providerURL
   * @param defaultBlocksToRetrieve
   * @param defaultNetworkId
   * @param defaultGas
   * @return
   * @throws Exception
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("save")
  @RolesAllowed("administrators")
  public Response saveSettings(GlobalSettings globalSettings) throws Exception {
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
