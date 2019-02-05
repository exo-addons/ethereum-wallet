package org.exoplatform.addon.ethereum.wallet.reward.rest;

import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.addon.ethereum.wallet.reward.model.RewardMemberDetail;
import org.exoplatform.addon.ethereum.wallet.reward.service.RewardService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to compute rewards
 */
@Path("/wallet/api/reward/compute")
@RolesAllowed({ "rewarding", "administrators" })
public class RewardBudgetREST implements ResourceContainer {
  private static final Log LOG = ExoLogger.getLogger(RewardBudgetREST.class);

  private RewardService    rewardService;

  public RewardBudgetREST(RewardService rewardService) {
    this.rewardService = rewardService;
  }

  /**
   * Compute reward per period
   * 
   * @param identityIds
   * @param periodDateInSeconds
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ "rewarding", "administrators" })
  public Response computeRewards(Set<Long> identityIds,
                                 @QueryParam("periodDateInSeconds") long periodDateInSeconds) {
    try {
      Set<RewardMemberDetail> rewards = rewardService.computeReward(identityIds, periodDateInSeconds);
      return Response.ok(rewards).build();
    } catch (Exception e) {
      LOG.warn("Error getting computed reward", e);
      return Response.status(500).type(MediaType.APPLICATION_JSON).entity("{error: " + e.getMessage() + "}").build();
    }
  }

}
