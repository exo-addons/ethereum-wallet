package org.exoplatform.addon.ethereum.wallet.rest;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.GamificationSettings;
import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.GamificationTeam;
import org.exoplatform.addon.ethereum.wallet.service.WalletGamificationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load extra Wallet data related to
 * Gamification addon
 */
@Path("/wallet/api/gamification")
public class WalletGamificationREST implements ResourceContainer {
  private static final Log  LOG = ExoLogger.getLogger(WalletGamificationREST.class);

  WalletGamificationService walletGamificationService;

  public WalletGamificationREST(WalletGamificationService walletGamificationService) {
    this.walletGamificationService = walletGamificationService;
  }

  /**
   * @return Gamification general settings
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("settings")
  @RolesAllowed("users")
  public Response getSettings() {
    try {
      GamificationSettings settings = walletGamificationService.getSettings();
      return Response.ok(settings == null ? new GamificationSettings() : settings).build();
    } catch (Exception e) {
      LOG.warn("Error getting gamification settings", e);
      return Response.serverError().build();
    }
  }

  /**
   * @return Gamification teams with their members
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("teams")
  @RolesAllowed("administrators")
  public Response getTeams() {
    return Response.ok(walletGamificationService.getTeams()).build();
  }

  /**
   * Remove a Gamification Team/Pool by id
   * 
   * @param id
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("removeTeam")
  @RolesAllowed("administrators")
  public Response removeTeam(@QueryParam("id") Long id) {
    if (id == null || id == 0) {
      return Response.status(400).build();
    }
    try {
      GamificationTeam team = walletGamificationService.removeTeam(id);
      LOG.info("{} removed Gamification pool {}", getCurrentUserId(), team.toString());
    } catch (Exception e) {
      LOG.warn("Error removing Gamification pool with id: " + id, e);
    }
    return Response.ok().build();
  }

  /**
   * Save global settings of gamification
   * 
   * @param gamificationSettings
   * @return
   */
  @POST
  @Path("saveSettings")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  public Response saveSettings(GamificationSettings gamificationSettings) {
    if (gamificationSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(400).build();
    }
    gamificationSettings = walletGamificationService.saveSettings(gamificationSettings);
    LOG.info("{} saved Gamification settings '{}'", getCurrentUserId(), gamificationSettings.toString());
    return Response.ok(gamificationSettings).build();
  }

  /**
   * Add/modifiy a gamification team
   * 
   * @param gamificationTeam
   * @return
   */
  @POST
  @Path("saveTeam")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  public Response saveTeam(GamificationTeam gamificationTeam) {
    if (gamificationTeam == null) {
      LOG.warn("Bad request sent to server with empty team");
      return Response.status(400).build();
    }
    try {
      gamificationTeam = walletGamificationService.saveTeam(gamificationTeam);
      LOG.info("{} saved Gamification pool {}", getCurrentUserId(), gamificationTeam.getName());
    } catch (Exception e) {
      LOG.warn("Error saving Gamification pool", e);
    }
    return Response.ok(gamificationTeam).build();
  }
}
