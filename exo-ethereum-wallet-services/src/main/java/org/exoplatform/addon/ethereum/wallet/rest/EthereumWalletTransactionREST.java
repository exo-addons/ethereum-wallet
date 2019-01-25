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

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletTransactionService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to retrieve detailed information about
 * users and spaces Ethereum transactions
 */
@Path("/wallet/api/transaction")
@RolesAllowed("users")
public class EthereumWalletTransactionREST implements ResourceContainer {

  private static final String              EMPTY_ADDRESS_ERROR = "Bad request sent to server with empty address {}";

  private static final Log                 LOG                 = ExoLogger.getLogger(EthereumWalletTransactionREST.class);

  private EthereumWalletTransactionService transactionService;

  public EthereumWalletTransactionREST(EthereumWalletTransactionService transactionService) {
    this.transactionService = transactionService;
  }

  /**
   * Store transaction hash in sender, receiver and contract accounts
   * 
   * @param transactionDetail
   * @return
   */
  @POST
  @Path("saveTransactionDetails")
  @RolesAllowed("users")
  public Response saveTransactionDetails(TransactionDetail transactionDetail) {
    if (transactionDetail == null || StringUtils.isBlank(transactionDetail.getHash())
        || StringUtils.isBlank(transactionDetail.getFrom())) {
      LOG.warn("Bad request sent to server with empty transaction details: {}",
               transactionDetail == null ? "" : transactionDetail.toString());
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    try {
      transactionService.saveTransactionDetail(transactionDetail, currentUserId, false);
    } catch (IllegalAccessException e) {
      LOG.warn("User {} is attempting to save transaction {}", currentUserId, transactionDetail, e);
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.warn("Error saving transaction message", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  /**
   * Get list of transactions of an address
   * 
   * @param networkId
   * @param address
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getTransactions")
  @RolesAllowed("users")
  public Response getTransactions(@QueryParam("networkId") long networkId,
                                  @QueryParam("address") String address,
                                  @QueryParam("administration") String administration) {
    if (networkId == 0) {
      LOG.warn("Bad request sent to server with empty networkId {}", networkId);
      return Response.status(400).build();
    }
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    try {
      boolean isAdministration = StringUtils.equals(administration, "true");
      List<TransactionDetail> transactionDetails = transactionService.getTransactions(networkId,
                                                                                      address,
                                                                                      currentUserId,
                                                                                      isAdministration);
      return Response.ok(transactionDetails).build();
    } catch (IllegalAccessException e) {
      LOG.warn("User {} attempts to display transactions of address {}", currentUserId, address);
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.warn("Error getting transactions of wallet " + address, e);
      return Response.serverError().build();
    }
  }
}
