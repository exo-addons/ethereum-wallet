package org.exoplatform.addon.ethereum.wallet.reward.rest;

import static org.exoplatform.addon.ethereum.wallet.utils.RewardUtils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.addon.ethereum.wallet.reward.model.RewardSettings;
import org.exoplatform.addon.ethereum.wallet.reward.service.RewardSettingsService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load reward settings
 */
@Path("/wallet/api/reward/settings")
@RolesAllowed({ "rewarding", "administrators" })
public class RewardSettingsREST implements ResourceContainer {
  private static final Log      LOG = ExoLogger.getLogger(RewardSettingsREST.class);

  private RewardSettingsService rewardSettingsService;

  public RewardSettingsREST(RewardSettingsService rewardSettingsService) {
    this.rewardSettingsService = rewardSettingsService;
  }

  /**
   * @return Reward general settings
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ "rewarding", "administrators" })
  public Response getSettings() {
    try {
      RewardSettings settings = rewardSettingsService.getSettings();
      return Response.ok(settings == null ? new RewardSettings() : settings).build();
    } catch (Exception e) {
      LOG.warn("Error getting reward settings", e);
      return Response.serverError().build();
    }
  }

  /**
   * Save global settings of reward
   * 
   * @param rewardSettings
   * @return
   */
  @POST
  @Path("save")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({ "rewarding", "administrators" })
  public Response saveSettings(RewardSettings rewardSettings) {
    if (rewardSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(400).build();
    }
    try {
      rewardSettingsService.saveSettings(rewardSettings);
      LOG.info("{} saved reward settings '{}'", getCurrentUserId(), rewardSettings.toString());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error saving reward settings", e);
      return Response.serverError().build();
    }
  }

}
