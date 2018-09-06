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

import org.exoplatform.addon.ethereum.wallet.model.AccountDetail;
import org.exoplatform.addon.ethereum.wallet.model.UserPreferences;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletStorage;
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

  private EthereumWalletStorage ethereumWalletStorage;

  public EthereumWalletAccountREST(EthereumWalletStorage ethereumWalletStorage) {
    this.ethereumWalletStorage = ethereumWalletStorage;
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
      AccountDetail accountDetail = ethereumWalletStorage.getUserDetails(id);
      if (accountDetail == null) {
        LOG.warn("User not found with id '{}'", id);
        return Response.status(400).build();
      }
      accountDetail.setAddress(ethereumWalletStorage.getUserAddress(id));
      return Response.ok(accountDetail).build();
    } else if (StringUtils.equals(type, SPACE_ACCOUNT_TYPE)) {
      AccountDetail accountDetail = ethereumWalletStorage.getSpaceDetails(id);
      if (accountDetail == null) {
        LOG.warn("Space not found with id '{}'", id);
        return Response.status(400).build();
      }
      accountDetail.setAddress(ethereumWalletStorage.getSpaceAddress(id));
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
    AccountDetail accountDetail = ethereumWalletStorage.getAccountDetailsByAddress(address);

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

    try {
      ethereumWalletStorage.saveWalletAddress(accountDetail, getCurrentUserId());
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
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
    ethereumWalletStorage.saveUserPreferences(getCurrentUserId(), userPreferences);

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

    List<String> userTransactions = ethereumWalletStorage.getAccountTransactions(networkId, address);
    JSONArray array = new JSONArray(userTransactions);

    return Response.ok(array.toString()).build();
  }
}
