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
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.isUserSpaceManager;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletAccountService;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to retrieve detailed information about
 * users and spaces with the associated Ethereum account addresses
 */
@Path("/wallet/api/account")
@RolesAllowed("users")
public class EthereumWalletAccountREST implements ResourceContainer {

  private static final String          EMPTY_ADDRESS_ERROR = "Bad request sent to server with empty address {}";

  private static final Log             LOG                 = ExoLogger.getLogger(EthereumWalletAccountREST.class);

  private EthereumWalletService        ethereumWalletService;

  private EthereumWalletAccountService accountService;

  public EthereumWalletAccountREST(EthereumWalletService ethereumWalletService, EthereumWalletAccountService accountService) {
    this.ethereumWalletService = ethereumWalletService;
    this.accountService = accountService;
  }

  /**
   * Retrieves the user or space details by username or spacePrettyName
   * 
   * @param remoteId
   * @param type
   * @return
   */
  @Path("detailsById")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getWalletByTypeAndID(@QueryParam("id") String remoteId, @QueryParam("type") String type) {
    if (StringUtils.isBlank(remoteId) || StringUtils.isBlank(type)) {
      LOG.warn("Bad request sent to server with id '{}' and type '{}'", remoteId, type);
      return Response.status(400).build();
    }
    try {
      Wallet wallet = accountService.getWallet(type, remoteId);
      if (wallet != null) {
        if (WalletType.isSpace(wallet.getType())) {
          wallet.setSpaceAdministrator(isUserSpaceManager(wallet.getId(), getCurrentUserId()));
        }
        wallet.setPassPhrase(null);
        return Response.ok(wallet).build();
      } else {
        return Response.ok("{}").build();
      }
    } catch (Exception e) {
      LOG.error("Error getting wallet by id {} and type {}", remoteId, type);
      return Response.serverError().build();
    }
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
  @RolesAllowed("users")
  public Response getWalletByAddress(@QueryParam("address") String address) {
    try {
      if (StringUtils.isBlank(address)) {
        LOG.warn(EMPTY_ADDRESS_ERROR, address);
        return Response.status(400).build();
      }
      Wallet wallet = accountService.getWalletByAddress(address);
      if (wallet != null) {
        if (WalletType.isSpace(wallet.getType())) {
          wallet.setSpaceAdministrator(isUserSpaceManager(wallet.getId(), getCurrentUserId()));
        }
        wallet.setPassPhrase(null);
        return Response.ok(wallet).build();
      } else {
        return Response.ok("{}").build();
      }
    } catch (Exception e) {
      LOG.error("Error getting wallet by address {}", address);
      return Response.serverError().build();
    }
  }

  /**
   * Retrieves the user or space details associated to an address
   * 
   * @param address
   * @return
   */
  @Path("remove")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  public Response removeAccountByAddress(@QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(400).build();
    }
    try {
      accountService.removeWalletByAddress(address, getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Can't delete address '{}' association", address, e);
      return Response.serverError().build();
    }
  }

  /**
   * Save address a user or space associated address
   * 
   * @param wallet
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveAddress")
  @RolesAllowed("users")
  public Response saveAddress(Wallet wallet) {
    if (wallet == null) {
      LOG.warn("Bad request sent to server with empty data");
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    LOG.info("User '{}' is saving new wallet address for {} '{}' with address '{}'",
             currentUserId,
             wallet.getType(),
             wallet.getId(),
             wallet.getAddress());
    try {
      accountService.saveWallet(wallet, currentUserId, true);
      return Response.ok(wallet.getPassPhrase()).build();
    } catch (IllegalAccessException | IllegalStateException e) {
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving address: User " + currentUserId + " attempts to save address of "
          + wallet.getType() + " '" + wallet.getId() + "' using address " + wallet.getAddress(), e);
      return Response.status(500).build();
    }
  }

  /**
   * Save user preferences of Wallet
   * 
   * @param preferences
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("savePreferences")
  @RolesAllowed("users")
  public Response savePreferences(WalletPreferences preferences) {
    if (preferences == null) {
      LOG.warn("Bad request sent to server with empty preferenes");
      return Response.status(400).build();
    }
    Long defaultGas = preferences.getDefaultGas();
    if (defaultGas == null || defaultGas == 0) {
      LOG.warn("Bad request sent to server with invalid preferenes defaultGas '{}'", defaultGas);
      return Response.status(400).build();
    }

    LOG.info("Saving user preferences '{}'", getCurrentUserId());
    try {
      ethereumWalletService.saveUserPreferences(getCurrentUserId(), preferences);
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving user preferences '" + getCurrentUserId() + "'", e);
      return Response.status(500).build();
    }
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
  @RolesAllowed("users")
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
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while user '" + getCurrentUserId() + "' requesting funds for wallet  '"
          + fundsRequest.getAddress() + "'", e);
      return Response.status(500).build();
    }
  }

  /**
   * Mark a notification as sent
   * 
   * @param notificationId
   * @return
   */
  @GET
  @Path("markFundRequestAsSent")
  @RolesAllowed("users")
  public Response markFundRequestAsSent(@QueryParam("notificationId") String notificationId) {
    if (StringUtils.isBlank(notificationId)) {
      LOG.warn("Bad request sent to server with empty notificationId");
      return Response.status(400).build();
    }

    String currentUser = getCurrentUserId();
    try {
      ethereumWalletService.markFundRequestAsSent(notificationId, currentUser);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while marking fund request with id '" + notificationId + "' for user '"
          + getCurrentUserId() + "'", e);
      return Response.status(500).build();
    }
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
  @RolesAllowed("users")
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
    try {
      return Response.ok(accountService.listWallets()).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving list of wallets", e);
      return Response.serverError().build();
    }
  }
}
