/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import static org.exoplatform.addon.ethereum.wallet.utils.Utils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.service.WalletTokenTransactionService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to manage transactions served by admin
 * wallet
 */
@Path("/wallet/api/admin/transaction")
@RolesAllowed("administrators")
public class WalletAdminTransactionREST implements ResourceContainer {

  private static final Log              LOG = ExoLogger.getLogger(WalletAdminTransactionREST.class);

  private WalletTokenTransactionService tokenOperationService;

  public WalletAdminTransactionREST(WalletTokenTransactionService tokenOperationService) {
    this.tokenOperationService = tokenOperationService;
  }

  /**
   * Initialize wallet identified by address with token and ether amounts
   * configured in initial funds
   * 
   * @param address Wallet address to initialize
   * @return REST response with status
   */
  @POST
  @Path("initializeWallet")
  @RolesAllowed("administrators")
  public Response initializeWallet(@FormParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Bad request sent to server with empty address to initialize");
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    try {
      tokenOperationService.initialize(address, currentUserId);
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error initializing wallet of {}", address, e);
      return Response.serverError().build();
    }
  }

  /**
   * Initialize wallet identified by address with token and ether amounts
   * provided in parameters
   * 
   * @param address Wallet address to initialize
   * @param tokenAmount
   * @param etherAmount
   * @param label
   * @param message
   * @return REST response with status
   */
  @POST
  @Path("initializeWalletWithSpecificParams")
  @RolesAllowed("administrators")
  public Response initializeWallet(@FormParam("address") String address,
                                   @FormParam("tokenAmount") double tokenAmount,
                                   @FormParam("etherAmount") double etherAmount,
                                   @FormParam("label") String label,
                                   @FormParam("message") String message) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Bad request sent to server with empty address to initialize");
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    try {
      tokenOperationService.initialize(address, tokenAmount, etherAmount, label, message, currentUserId);
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error initializing wallet of {}", address, e);
      return Response.serverError().build();
    }
  }

}
