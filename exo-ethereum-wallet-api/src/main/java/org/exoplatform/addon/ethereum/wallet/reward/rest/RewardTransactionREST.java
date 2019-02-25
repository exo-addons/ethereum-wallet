package org.exoplatform.addon.ethereum.wallet.reward.rest;

import static org.exoplatform.addon.ethereum.wallet.utils.RewardUtils.timeFromSeconds;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.reward.model.*;
import org.exoplatform.addon.ethereum.wallet.reward.service.RewardTransactionService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load reward transactions
 */
@Path("/wallet/api/reward/transaction")
@RolesAllowed({ "rewarding", "administrators" })
public class RewardTransactionREST implements ResourceContainer {
  private static final Log         LOG = ExoLogger.getLogger(RewardTransactionREST.class);

  private RewardTransactionService rewardTransactionService;

  public RewardTransactionREST(RewardTransactionService rewardTransactionService) {
    this.rewardTransactionService = rewardTransactionService;
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
  @Path("list")
  @RolesAllowed({ "rewarding", "administrators" })
  public Response getRewardTransactions(@QueryParam("networkId") long networkId,
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

    List<JSONObject> periodTransactions = rewardTransactionService.getRewardTransactions(networkId,
                                                                                         periodType,
                                                                                         startDateInSeconds);
    JSONArray array = new JSONArray(periodTransactions);
    return Response.ok(array.toString()).build();
  }

  /**
   * @param rewardTransactions list of finished rewardTransactions
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveRewardTransactions")
  @RolesAllowed({ "rewarding", "administrators" })
  public Response saveRewardTransactions(List<RewardTransaction> rewardTransactions) {
    if (rewardTransactions == null || rewardTransactions.isEmpty()) {
      LOG.warn("Bad request sent to server with empty transactions to save: {}", rewardTransactions);
      return Response.status(400).build();
    }

    try {
      for (RewardTransaction rewardTransaction : rewardTransactions) {
        rewardTransactionService.saveRewardTransaction(rewardTransaction);
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
  @Path("saveRewardTransaction")
  @RolesAllowed({ "rewarding", "administrators" })
  public Response saveRewardTransaction(RewardTransaction rewardTransaction) {
    if (rewardTransaction == null) {
      LOG.warn("Bad request sent to server with empty transaction to save: {}", rewardTransaction);
      return Response.status(400).build();
    }

    try {
      rewardTransactionService.saveRewardTransaction(rewardTransaction);
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
  @Path("getDates")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ "rewarding", "administrators" })
  public Response getRewardDates(@QueryParam("periodType") String periodType,
                                 @QueryParam("dateInSeconds") long dateInSeconds) {
    if (dateInSeconds == 0) {
      LOG.warn("Bad request sent to server with empty 'dateInSeconds' parameter");
      return Response.status(400).build();
    }
    if (StringUtils.isBlank(periodType)) {
      LOG.warn("Bad request sent to server with empty 'periodType' parameter");
      return Response.status(400).build();
    }
    RewardPeriodType rewardPeriodType = RewardPeriodType.valueOf(periodType);
    RewardPeriod rewardPeriod = rewardPeriodType.getPeriodOfTime(timeFromSeconds(dateInSeconds));
    return Response.ok(rewardPeriod.toString()).build();
  }

}
