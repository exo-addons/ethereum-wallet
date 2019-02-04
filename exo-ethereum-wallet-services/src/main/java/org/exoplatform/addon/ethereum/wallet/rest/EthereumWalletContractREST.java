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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.ContractDetail;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletContractService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/delete a contract as default
 * displayed contracts for end users
 */
@Path("/wallet/api/contract")
@RolesAllowed("users")
public class EthereumWalletContractREST implements ResourceContainer {

  private static final Log              LOG = ExoLogger.getLogger(EthereumWalletContractREST.class);

  private EthereumWalletContractService contractService;

  public EthereumWalletContractREST(EthereumWalletContractService contractService) {
    this.contractService = contractService;
  }

  /**
   * Return saved contract details by address
   * 
   * @param address
   * @param networkId
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getContract")
  @RolesAllowed("users")
  public Response getContract(@QueryParam("address") String address, @QueryParam("networkId") Long networkId) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Empty contract address");
      return Response.status(400).build();
    }
    try {
      ContractDetail contractDetail = contractService.getContractDetail(address, networkId);
      if (contractDetail == null) {
        contractDetail = new ContractDetail();
      }
      return Response.ok(contractDetail).build();
    } catch (Exception e) {
      LOG.warn("Error getting contract details: " + address + " on network with id " + networkId, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("bin/{name}")
  @RolesAllowed("reward")
  public Response getBin(@PathParam("name") String name) {
    if (StringUtils.isBlank(name)) {
      LOG.warn("Empty resource name");
      return Response.status(400).build();
    }
    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
      LOG.error(getCurrentUserId() + " has used a forbidden path character is used: '..' or '/' or '\\'");
      return Response.status(403).build();
    }
    try {
      String contractAbi = contractService.getContract(name, "bin");
      return Response.ok(contractAbi).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving contract BIN: " + name, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("abi/{name}")
  @RolesAllowed("reward")
  public Response getAbi(@PathParam("name") String name) {
    if (StringUtils.isBlank(name)) {
      LOG.warn("Empty resource name");
      return Response.status(400).build();
    }
    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
      LOG.error(getCurrentUserId() + " has used a forbidden path character is used: '..' or '/' or '\\'");
      return Response.status(403).build();
    }
    try {
      String contractAbi = contractService.getContract(name, "json");
      return Response.ok(contractAbi).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving contract ABI: " + name, e);
      return Response.serverError().build();
    }
  }

  /**
   * Save a new contract address to display it in wallet of all users and save
   * contract name and symbol
   * 
   * @param contractDetail
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("save")
  @RolesAllowed("reward")
  public Response saveContract(ContractDetail contractDetail) {
    if (contractDetail == null) {
      LOG.warn("Can't save empty contract");
      return Response.status(400).build();
    }
    if (contractDetail.getAddress() == null) {
      LOG.warn("Can't save empty address for contract");
      return Response.status(400).build();
    }
    if (contractDetail.getNetworkId() == null || contractDetail.getNetworkId() == 0) {
      LOG.warn("Can't remove empty network id for contract");
      return Response.status(400).build();
    }
    contractDetail.setAddress(contractDetail.getAddress().toLowerCase());
    try {
      contractService.saveContract(contractDetail);
      LOG.info("{} saved contract details '{}'", getCurrentUserId(), contractDetail.toJSONString());
    } catch (Exception e) {
      LOG.warn("Error saving contract as default: " + contractDetail.getAddress(), e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  /**
   * Removes a contract address from default contracts displayed in wallet of
   * all users
   * 
   * @param address
   * @param networkId
   * @return
   */
  @POST
  @Path("remove")
  @RolesAllowed("reward")
  public Response removeContract(@FormParam("address") String address, @FormParam("networkId") Long networkId) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Can't remove empty address for contract");
      return Response.status(400).build();
    }
    if (networkId == null || networkId == 0) {
      LOG.warn("Can't remove empty network id for contract");
      return Response.status(400).build();
    }
    try {
      contractService.removeDefaultContract(address.toLowerCase(), networkId);
      LOG.info("{} removed contract details '{}'", getCurrentUserId(), address);
    } catch (Exception e) {
      LOG.warn("Error removing default contract: " + address, e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

}
