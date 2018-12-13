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

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.exoplatform.addon.ethereum.wallet.service.WalletKudosService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load extra Wallet data related to
 * Kudos addon
 */
@Path("/wallet/api/kudos")
public class WalletKudosREST implements ResourceContainer {
  private static final Log   LOG = ExoLogger.getLogger(WalletKudosREST.class);

  private WalletKudosService extendedWalletService;

  public WalletKudosREST(WalletKudosService extendedWalletService) {
    this.extendedWalletService = extendedWalletService;
  }

  /**
   * Get tokens amount per Kudos
   * 
   * @return
   */
  @Path("getKudosBudget")
  @GET
  @RolesAllowed("administrators")
  public Response getKudosBudget() {
    return Response.ok(String.valueOf(extendedWalletService.getKudosBudget())).build();
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
   * @param budget
   */
  @POST
  @Path("saveKudosTotalBudget")
  @RolesAllowed("administrators")
  public Response saveKudosTotalBudget(@FormParam("budget") double budget) {
    extendedWalletService.saveKudosTotalBudget(budget);
    LOG.info("{} saved kudos total budget '{}'", getCurrentUserId(), budget);
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
    LOG.info("{} saved kudos contract '{}'", getCurrentUserId(), kudosContractAddress);
    return Response.ok().build();
  }
}
