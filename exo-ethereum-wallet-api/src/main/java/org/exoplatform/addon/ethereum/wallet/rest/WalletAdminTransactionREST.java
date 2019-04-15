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

import static org.exoplatform.addon.ethereum.wallet.utils.WalletUtils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
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

  private static final String           BAD_REQUEST_SENT_TO_SERVER_BY = "Bad request sent to server by '";

  private static final Log              LOG                           = ExoLogger.getLogger(WalletAdminTransactionREST.class);

  private WalletTokenTransactionService tokenOperationService;

  public WalletAdminTransactionREST(WalletTokenTransactionService tokenOperationService) {
    this.tokenOperationService = tokenOperationService;
  }

  /**
   * Send transaction to wallet identified by address with possible transaction
   * types: - initialize - approve - disapprove
   * 
   * @param address Wallet address to process
   * @param action Wallet address to process
   * @return REST response with status
   */
  @POST
  @RolesAllowed("administrators")
  public Response executeTransactionOnWallet(@FormParam("action") String action, @FormParam("address") String address) {
    String currentUserId = getCurrentUserId();
    if (StringUtils.isBlank(address)) {
      LOG.warn(BAD_REQUEST_SENT_TO_SERVER_BY + currentUserId + "' with empty address");
      return Response.status(400).build();
    }
    if (StringUtils.isBlank(action)) {
      LOG.warn(BAD_REQUEST_SENT_TO_SERVER_BY + currentUserId + "' with empty action");
      return Response.status(400).build();
    }

    TransactionDetail transactionDetail = null;
    try {
      if (StringUtils.equals(action, "initialize")) {
        transactionDetail = tokenOperationService.initialize(address, currentUserId);
      } else if (StringUtils.equals(action, "approve")) {
        transactionDetail = tokenOperationService.approveAccount(address, currentUserId);
      } else if (StringUtils.equals(action, "disapprove")) {
        transactionDetail = tokenOperationService.disapproveAccount(address, currentUserId);
      } else {
        LOG.warn(BAD_REQUEST_SENT_TO_SERVER_BY + currentUserId + "' with action: " + action);
        return Response.status(400).build();
      }
      return Response.ok(transactionDetail == null ? "" : transactionDetail.getHash()).build();
    } catch (Exception e) {
      LOG.warn("Error processing action {} on wallet {}", action, address, e);
      return Response.serverError().build();
    }
  }

}
