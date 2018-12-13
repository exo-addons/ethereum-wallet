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

import org.exoplatform.addon.ethereum.wallet.ext.reward.model.*;
import org.exoplatform.addon.ethereum.wallet.service.WalletRewardService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load reward transactions
 */
@Path("/wallet/api/reward")
public class WalletRewardREST implements ResourceContainer {
  private static final Log    LOG = ExoLogger.getLogger(WalletRewardREST.class);

  private WalletRewardService rewardService;

  public WalletRewardREST(WalletRewardService walletRewardService) {
    this.rewardService = walletRewardService;
  }

  /**
   * Return period transaction
   * 
   * @param networkId
   * @param periodType
   * @param startDateInSeconds
   * @param rewardType
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getPeriodRewardTransactions")
  @RolesAllowed("users")
  public Response getPeriodRewardTransactions(@QueryParam("networkId") long networkId,
                                              @QueryParam("periodType") String periodType,
                                              @QueryParam("startDateInSeconds") long startDateInSeconds,
                                              @QueryParam("rewardType") String rewardType) {
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
                                                                                    rewardType);
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
