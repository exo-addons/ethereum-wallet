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

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.ext.kudos.model.KudosTransaction;
import org.exoplatform.addon.ethereum.wallet.service.ExtendedWalletService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load extra Wallet data related to
 * other addons
 */
@Path("/wallet/api/ext")
public class ExtraWalletSettingsREST implements ResourceContainer {
  private static final Log      LOG = ExoLogger.getLogger(ExtraWalletSettingsREST.class);

  private ExtendedWalletService extendedWalletService;

  public ExtraWalletSettingsREST(ExtendedWalletService extendedWalletService) {
    this.extendedWalletService = extendedWalletService;
  }

  /**
   * Get tokens amount per Kudos
   * 
   * @return
   */
  @Path("getTokensPerKudos")
  @GET
  @RolesAllowed("administrators")
  public Response getTokensPerKudos() {
    return Response.ok(String.valueOf(extendedWalletService.getTokensPerKudos())).build();
  }

  /**
   * Get contract address for Token rewards payment
   * 
   * @return
   */
  @Path("getKudosContract")
  @GET
  @RolesAllowed("administrators")
  public Response getKudosContract() {
    return Response.ok(String.valueOf(extendedWalletService.getKudosContract())).build();
  }

  /**
   * Save tokens amount per Kudos setting
   * 
   * @param tokensPerKudos
   */
  @POST
  @Path("saveTokensPerKudos")
  @RolesAllowed("administrators")
  public Response saveTokensPerKudos(@FormParam("tokensPerKudos") double tokensPerKudos) {
    extendedWalletService.saveTokensPerKudos(tokensPerKudos);
    return Response.ok().build();
  }

  /**
   * Save kudos contract address used for rewards payment
   * 
   * @param kudosContractAddress
   */
  @POST
  @Path("saveKudosContract")
  @RolesAllowed("administrators")
  public Response saveKudosContract(@FormParam("kudosContract") String kudosContractAddress) {
    extendedWalletService.saveKudosContract(kudosContractAddress);
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

    List<JSONObject> periodTransactions = extendedWalletService.getPeriodTransactions(networkId, periodType, startDateInSeconds);
    JSONArray array = new JSONArray(periodTransactions);
    return Response.ok(array.toString()).build();
  }

  /**
   * @param kudosTransactions list of finished kudosTransactions
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("savePeriodKudosTransactions")
  @RolesAllowed("users")
  public Response savePeriodKudosTransactions(List<KudosTransaction> kudosTransactions) {
    if (kudosTransactions == null || kudosTransactions.isEmpty()) {
      LOG.warn("Bad request sent to server with empty transactions to save: {}", kudosTransactions);
      return Response.status(400).build();
    }

    try {
      for (KudosTransaction kudosTransaction : kudosTransactions) {
        extendedWalletService.savePeriodKudosTransaction(kudosTransaction);
      }
    } catch (Exception e) {
      LOG.warn("Error saving transaction", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  /**
   * @param kudosTransaction kudosTransaction to save in kudos perid of time
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("savePeriodKudosTransaction")
  @RolesAllowed("users")
  public Response savePeriodKudosTransaction(KudosTransaction kudosTransaction) {
    if (kudosTransaction == null) {
      LOG.warn("Bad request sent to server with empty transaction to save: {}", kudosTransaction);
      return Response.status(400).build();
    }

    try {
      extendedWalletService.savePeriodKudosTransaction(kudosTransaction);
    } catch (Exception e) {
      LOG.warn("Error saving transaction", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }
}
