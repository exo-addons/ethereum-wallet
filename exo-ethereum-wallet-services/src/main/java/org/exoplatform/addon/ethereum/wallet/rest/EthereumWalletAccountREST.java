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

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to retrieve detailed inormation about
 * users and spaces with the associated Ethereum account addresses
 */
@Path("/wallet/api/account")
@RolesAllowed("users")
public class EthereumWalletAccountREST implements ResourceContainer {

  private static final Log      LOG = ExoLogger.getLogger(EthereumWalletAccountREST.class);

  private EthereumWalletService ethereumWalletService;

  public EthereumWalletAccountREST(EthereumWalletService ethereumWalletService) {
    this.ethereumWalletService = ethereumWalletService;
  }

  /**
   * Retrieves the user or space details by username or spacePrettyName
   * 
   * @param id
   * @param type
   * @return
   */
  @Path("detailsById")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAccountByTypeAndID(@QueryParam("id") String id, @QueryParam("type") String type) {
    if (StringUtils.isBlank(id) || StringUtils.isBlank(type)
        || !(StringUtils.equals(type, USER_ACCOUNT_TYPE) || StringUtils.equals(type, SPACE_ACCOUNT_TYPE))) {
      LOG.warn("Bad request sent to server with id '{}' and type '{}'", id, type);
      return Response.status(400).build();
    }
    if (StringUtils.equals(type, USER_ACCOUNT_TYPE)) {
      AccountDetail accountDetail = ethereumWalletService.getUserDetails(id);
      if (accountDetail == null) {
        LOG.warn("User not found with id '{}'", id);
        return Response.status(400).build();
      }
      accountDetail.setAddress(ethereumWalletService.getUserAddress(id));
      return Response.ok(accountDetail).build();
    } else if (StringUtils.equals(type, SPACE_ACCOUNT_TYPE)) {
      AccountDetail accountDetail = ethereumWalletService.getSpaceDetails(id);
      if (accountDetail == null) {
        LOG.warn("Space not found with id '{}'", id);
        return Response.status(400).build();
      }
      accountDetail.setAddress(ethereumWalletService.getSpaceAddress(id));
      return Response.ok(accountDetail).build();
    }

    return Response.ok("{}").build();
  }

  /**
   * Retrieves the user or space details associated to an address
   * 
   * @param address
   * @return
   */
  @Path("detailsByAddress")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAccountByAddress(@QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Bad request sent to server with empty address {}", address);
      return Response.status(400).build();
    }

    address = address.toLowerCase();
    AccountDetail accountDetail = ethereumWalletService.getAccountDetailsByAddress(address);

    return Response.ok(accountDetail == null ? "{}" : accountDetail).build();
  }

  /**
   * Save address a user or space associated address
   * 
   * @param accountDetail
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveAddress")
  public Response saveAddress(AccountDetail accountDetail) {
    if (accountDetail == null) {
      LOG.warn("Bad request sent to server with empty data");
      return Response.status(400).build();
    }

    LOG.info("User '{}' is saving new wallet address for {} '{}' with address '{}'",
             getCurrentUserId(),
             accountDetail.getType(),
             accountDetail.getId(),
             accountDetail.getAddress());
    try {
      String securityPhrase = ethereumWalletService.saveWalletAddress(accountDetail);
      return Response.ok(securityPhrase).build();
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving address: User " + getCurrentUserId() + " attempts to save address of "
          + accountDetail.getType() + " '" + accountDetail.getId() + "' using address " + accountDetail.getAddress(), e);
      return Response.status(500).build();
    }
  }

  /**
   * Save user preferences of Wallet
   * 
   * @param userPreferences
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("savePreferences")
  public Response savePreferences(UserPreferences userPreferences) {
    if (userPreferences == null) {
      LOG.warn("Bad request sent to server with empty preferenes");
      return Response.status(400).build();
    }
    Integer defaultGas = userPreferences.getDefaultGas();
    if (defaultGas == null || defaultGas == 0) {
      LOG.warn("Bad request sent to server with invalid preferenes defaultGas '{}'", defaultGas);
      return Response.status(400).build();
    }

    LOG.info("Saving user preferences '{}'", getCurrentUserId());
    try {
      ethereumWalletService.saveUserPreferences(getCurrentUserId(), userPreferences);
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving user preferences '" + getCurrentUserId() + "'", e);
      return Response.status(500).build();
    }

    return Response.ok().build();
  }

  /**
   * Sends a fund request notifications
   * 
   * @param fundsRequest
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("requestFunds")
  public Response requestFunds(FundsRequest fundsRequest) {
    if (fundsRequest == null) {
      LOG.warn("Bad request sent to server with empty funds request");
      return Response.status(400).build();
    }

    if (StringUtils.isBlank(fundsRequest.getAddress())) {
      LOG.warn("Bad request sent to server with empty sender address");
      return Response.status(400).build();
    }

    String receipientRemoteId = fundsRequest.getReceipient();
    String receipientType = fundsRequest.getReceipientType();

    if (StringUtils.isBlank(receipientRemoteId) || StringUtils.isBlank(receipientType)) {
      LOG.warn("Bad request sent to server with empty receipient");
      return Response.status(400).build();
    }

    try {
      ethereumWalletService.requestFunds(fundsRequest);
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while user '" + getCurrentUserId() + "' requesting funds for wallet  '"
          + fundsRequest.getAddress() + "'", e);
      return Response.status(500).build();
    }

    return Response.ok().build();
  }

  /**
   * Mark a notification as sent
   * 
   * @param notificationId
   * @return
   */
  @GET
  @Path("markFundRequestAsSent")
  public Response markFundRequestAsSent(@QueryParam("notificationId") String notificationId) {
    if (StringUtils.isBlank(notificationId)) {
      LOG.warn("Bad request sent to server with empty notificationId");
      return Response.status(400).build();
    }

    String currentUser = getCurrentUserId();
    try {
      ethereumWalletService.markFundRequestAsSent(notificationId, currentUser);
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while marking fund request with id '" + notificationId + "' for user '"
          + getCurrentUserId() + "'", e);
      return Response.status(500).build();
    }

    return Response.ok().build();
  }

  /**
   * Returns fund request status
   * 
   * @param notificationId
   * @return
   */
  @GET
  @Path("fundRequestSent")
  @Produces(MediaType.TEXT_PLAIN)
  public Response isFundRequestSent(@QueryParam("notificationId") String notificationId) {
    if (StringUtils.isBlank(notificationId)) {
      LOG.warn("Bad request sent to server with empty notificationId");
      return Response.status(400).build();
    }

    String currentUser = getCurrentUserId();
    try {
      boolean fundRequestSent = ethereumWalletService.isFundRequestSent(notificationId, currentUser);
      return Response.ok(String.valueOf(fundRequestSent)).build();
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving fund request status", e);
      return Response.serverError().build();
    }
  }

  /**
   * Get list of wallet accounts
   * 
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("list")
  @RolesAllowed("administrators")
  public Response getWallets() {
    List<AccountDetail> accounts;
    try {
      accounts = ethereumWalletService.lisWallets();
      return Response.ok(accounts).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving list of wallets", e);
      return Response.serverError().build();
    }
  }

  /**
   * Store transaction custom label and message
   * 
   * @param transactionMessage
   * @return
   */
  @POST
  @Path("saveTransactionMessage")
  public Response saveTransactionMessage(TransactionMessage transactionMessage) {
    if (transactionMessage == null || StringUtils.isBlank(transactionMessage.getHash())) {
      LOG.warn("Bad request sent to server with empty transaction hash");
      return Response.status(400).build();
    }

    transactionMessage.setSender(getCurrentUserId());
    try {
      ethereumWalletService.saveTransactionMessage(transactionMessage);
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
  public Response getTransactions(@QueryParam("networkId") long networkId, @QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Bad request sent to server with empty address {}", address);
      return Response.status(400).build();
    }
    if (networkId == 0) {
      LOG.warn("Bad request sent to server with empty networkId {}", networkId);
      return Response.status(400).build();
    }

    List<JSONObject> userTransactions = ethereumWalletService.getAccountTransactions(networkId, address);
    JSONArray array = new JSONArray(userTransactions);

    return Response.ok(array.toString()).build();
  }
}
