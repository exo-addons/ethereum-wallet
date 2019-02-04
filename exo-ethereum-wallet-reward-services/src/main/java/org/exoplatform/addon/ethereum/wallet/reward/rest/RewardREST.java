package org.exoplatform.addon.ethereum.wallet.reward.rest;

import static org.exoplatform.addon.ethereum.wallet.reward.service.utils.RewardUtils.timeFromSeconds;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.getCurrentUserId;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.reward.model.*;
import org.exoplatform.addon.ethereum.wallet.reward.service.RewardService;
import org.exoplatform.addon.ethereum.wallet.service.WalletGamificationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load extra Wallet data related to
 * Gamification addon
 */
@Path("/wallet/api/reward")
public class RewardREST implements ResourceContainer {
  private static final Log  LOG = ExoLogger.getLogger(RewardREST.class);

  private RewardService rewardService;

  public RewardREST(RewardService rewardService) {
    this.rewardService = rewardService;
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
      RewardSettings settings = walletGamificationService.getSettings();
      return Response.ok(settings == null ? new RewardSettings() : settings).build();
    } catch (Exception e) {
      LOG.warn("Error getting gamification settings", e);
      return Response.serverError().build();
    }
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
  @RolesAllowed("reward")
  public Response saveSettings(RewardSettings gamificationSettings) {
    if (gamificationSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(400).build();
    }
    try {
      gamificationSettings = walletGamificationService.saveSettings(gamificationSettings);
      LOG.info("{} saved Gamification settings '{}'", getCurrentUserId(), gamificationSettings.toString());
      return Response.ok(gamificationSettings).build();
    } catch (Exception e) {
      LOG.warn("Error saving Gamification settings", e);
      return Response.serverError().build();
    }
  }

  /**
   * @return Gamification teams with their members
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("teams")
  @RolesAllowed("reward")
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
  @RolesAllowed("reward")
  public Response removeTeam(@QueryParam("id") Long id) {
    if (id == null || id == 0) {
      return Response.status(400).build();
    }
    try {
      RewardTeam team = walletGamificationService.removeTeam(id);
      LOG.info("{} removed Gamification pool {}", getCurrentUserId(), team.toString());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error removing Gamification pool with id: " + id, e);
      return Response.serverError().build();
    }
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
  @RolesAllowed("reward")
  public Response saveTeam(RewardTeam gamificationTeam) {
    if (gamificationTeam == null) {
      LOG.warn("Bad request sent to server with empty team");
      return Response.status(400).build();
    }
    try {
      gamificationTeam = walletGamificationService.saveTeam(gamificationTeam);
      LOG.info("{} saved Gamification pool {}", getCurrentUserId(), gamificationTeam.getName());
      return Response.ok(gamificationTeam).build();
    } catch (Exception e) {
      LOG.warn("Error saving Gamification pool", e);
      return Response.serverError().build();
    }
  }


  /**
   * Return period transaction
   * 
   * @param networkId
   * @param periodType
   * @param startDateInSeconds
   * @param walletRewardType : KUDOS_PERIOD_TRANSACTIONS, GAMIFICATION_PERIOD_TRANSACTIONS ...
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getPeriodRewardTransactions")
  @RolesAllowed("users")
  public Response getPeriodRewardTransactions(@QueryParam("networkId") long networkId,
                                              @QueryParam("periodType") String periodType,
                                              @QueryParam("startDateInSeconds") long startDateInSeconds,
                                              @QueryParam("walletRewardType") String walletRewardType) {
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

    List<JSONObject> periodTransactions = rewardService.getPeriodRewardTransactions(networkId,
                                                                                    periodType,
                                                                                    startDateInSeconds,
                                                                                    walletRewardType);
    JSONArray array = new JSONArray(periodTransactions);
    return Response.ok(array.toString()).build();
  }

  /**
   * @param rewardTransactions list of finished rewardTransactions
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("savePeriodRewardTransactions")
  @RolesAllowed("users")
  public Response savePeriodRewardTransactions(List<RewardTransaction> rewardTransactions) {
    if (rewardTransactions == null || rewardTransactions.isEmpty()) {
      LOG.warn("Bad request sent to server with empty transactions to save: {}", rewardTransactions);
      return Response.status(400).build();
    }

    try {
      for (RewardTransaction rewardTransaction : rewardTransactions) {
        rewardService.savePeriodRewardTransaction(rewardTransaction);
      }
    } catch (Exception e) {
      LOG.warn("Error saving transaction", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  /**
   * @param rewardTransaction rewardTransaction to save in reward period of time
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("savePeriodRewardTransaction")
  @RolesAllowed("users")
  public Response savePeriodRewardTransaction(RewardTransaction rewardTransaction) {
    if (rewardTransaction == null) {
      LOG.warn("Bad request sent to server with empty transaction to save: {}", rewardTransaction);
      return Response.status(400).build();
    }

    try {
      rewardService.savePeriodRewardTransaction(rewardTransaction);
    } catch (Exception e) {
      LOG.warn("Error saving transaction", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  /**
   * Retrieves reward perdiod start and end dates
   * 
   * @param periodType
   * @param dateInSeconds
   * @return
   */
  @Path("getPeriodRewardDates")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getPeriodRewardDates(@QueryParam("periodType") String periodType,
                                       @QueryParam("dateInSeconds") long dateInSeconds) {
    if (dateInSeconds == 0) {
      LOG.warn("Bad request sent to server with empty 'dateInSeconds' parameter");
      return Response.status(400).build();
    }
    if (StringUtils.isBlank(periodType)) {
      LOG.warn("Bad request sent to server with empty 'periodType' parameter");
      return Response.status(400).build();
    }
    RewardPeriodType gamificationPeriodType = RewardPeriodType.valueOf(periodType);
    RewardPeriod gamificationPeriod = gamificationPeriodType.getPeriodOfTime(timeFromSeconds(dateInSeconds));
    return Response.ok(gamificationPeriod.toString()).build();
  }

}
