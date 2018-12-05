package org.exoplatform.addon.ethereum.wallet.rest;

import static org.exoplatform.addon.ethereum.wallet.service.utils.GamificationUtils.timeFromSeconds;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.*;
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
    return Response.ok(walletGamificationService.getSettings()).build();
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
   * Save global settings of gamification
   * 
   * @param gamificationSettings
   * @return
   */
  @POST
  @Path("saveSettings")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  public Response saveSettings(GamificationSettings gamificationSettings) {
    if (gamificationSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(400).build();
    }
    walletGamificationService.saveSettings(gamificationSettings);
    return Response.ok().build();
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
  @RolesAllowed("administrators")
  public Response saveTeam(GamificationTeam gamificationTeam) {
    if (gamificationTeam == null) {
      LOG.warn("Bad request sent to server with empty team");
      return Response.status(400).build();
    }
    walletGamificationService.saveTeam(gamificationTeam);
    return Response.ok().build();
  }

  /**
   * Return period transaction
   * 
   * @param networkId
   * @param periodType
   * @param startDateInSeconds
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getPeriodTransactions")
  @RolesAllowed("users")
  public Response getPeriodTransactions(@QueryParam("networkId") long networkId,
                                        @QueryParam("periodType") String periodType,
                                        @QueryParam("startDateInSeconds") long startDateInSeconds) {
    if (networkId == 0) {
      LOG.warn("Bad request sent to server with empty networkId {}", networkId);
      return Response.status(400).build();
    }
    if (StringUtils.isBlank(periodType)) {
      LOG.warn("Bad request sent to server with empty periodType {}", periodType);
      return Response.status(400).build();
    }
    if (startDateInSeconds == 0) {
      LOG.warn("Bad request sent to server with empty startDateInSeconds {}", startDateInSeconds);
      return Response.status(400).build();
    }

    List<JSONObject> periodTransactions = walletGamificationService.getPeriodTransactions(networkId,
                                                                                          periodType,
                                                                                          startDateInSeconds);
    JSONArray array = new JSONArray(periodTransactions);
    return Response.ok(array.toString()).build();
  }

  /**
   * @param gamificationTransactions list of finished gamification transactions
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("savePeriodTransactions")
  @RolesAllowed("users")
  public Response savePeriodTransactions(List<GamificationTransaction> gamificationTransactions) {
    if (gamificationTransactions == null || gamificationTransactions.isEmpty()) {
      LOG.warn("Bad request sent to server with empty transactions to save: {}", gamificationTransactions);
      return Response.status(400).build();
    }

    try {
      for (GamificationTransaction gamificationTransaction : gamificationTransactions) {
        walletGamificationService.savePeriodTransaction(gamificationTransaction);
      }
    } catch (Exception e) {
      LOG.warn("Error saving transaction", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  /**
   * @param gamificationTransaction to save in gamification perid of time
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("savePeriodTransaction")
  @RolesAllowed("users")
  public Response savePeriodTransaction(GamificationTransaction gamificationTransaction) {
    if (gamificationTransaction == null) {
      LOG.warn("Bad request sent to server with empty transaction to save: {}", gamificationTransaction);
      return Response.status(400).build();
    }

    try {
      walletGamificationService.savePeriodTransaction(gamificationTransaction);
    } catch (Exception e) {
      LOG.warn("Error saving transaction", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  /**
   * Retrieves all kudos by a period of a designed time
   * 
   * @param periodType
   * @param dateInSeconds
   * @return
   */
  @Path("getPeriodDates")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getPeriodDates(@QueryParam("periodType") String periodType, @QueryParam("dateInSeconds") long dateInSeconds) {
    if (dateInSeconds == 0) {
      LOG.warn("Bad request sent to server with empty 'dateInSeconds' parameter");
      return Response.status(400).build();
    }
    if (StringUtils.isBlank(periodType)) {
      LOG.warn("Bad request sent to server with empty 'periodType' parameter");
      return Response.status(400).build();
    }
    GamificationPeriodType gamificationPeriodType = GamificationPeriodType.valueOf(periodType);
    GamificationPeriod gamificationPeriod = gamificationPeriodType.getPeriodOfTime(timeFromSeconds(dateInSeconds));
    return Response.ok(gamificationPeriod.toString()).build();
  }
}
