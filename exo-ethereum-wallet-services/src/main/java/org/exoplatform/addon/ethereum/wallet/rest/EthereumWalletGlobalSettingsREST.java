package org.exoplatform.addon.ethereum.wallet.rest;

import static org.exoplatform.addon.ethereum.wallet.rest.Utils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load global settings such as
 * ethereum network and default contracts
 */
@Path("/wallet/api/global-settings")
public class EthereumWalletGlobalSettingsREST implements ResourceContainer {

  private static final Log      LOG = ExoLogger.getLogger(EthereumWalletGlobalSettingsREST.class);

  private EthereumWalletStorage ethereumWalletStorage;

  public EthereumWalletGlobalSettingsREST(EthereumWalletStorage ethereumWalletStorage) {
    this.ethereumWalletStorage = ethereumWalletStorage;
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
    GlobalSettings globalSettings = ethereumWalletStorage.getSettings(networkId, getCurrentUserId());

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

    ethereumWalletStorage.saveSettings(globalSettings);
    return Response.ok().build();
  }
}
