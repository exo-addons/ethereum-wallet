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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.ContractDetail;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/delete a contract as default
 * displayed contracts for end users
 */
@Path("/wallet/api/contract")
@RolesAllowed("administrators")
public class EthereumWalletContractREST implements ResourceContainer {

  private static final Log      LOG = ExoLogger.getLogger(EthereumWalletContractREST.class);

  private EthereumWalletService ethereumWalletService;

  public EthereumWalletContractREST(EthereumWalletService ethereumWalletService) {
    this.ethereumWalletService = ethereumWalletService;
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
    ethereumWalletService.saveDefaultContract(contractDetail);
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
  public Response removeContract(@FormParam("address") String address, @FormParam("networkId") Long networkId) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Can't remove empty address for contract");
      return Response.status(400).build();
    }
    if (networkId == null || networkId == 0) {
      LOG.warn("Can't remove empty network id for contract");
      return Response.status(400).build();
    }
    ethereumWalletService.removeDefaultContract(address.toLowerCase(), networkId);
    return Response.ok().build();
  }
}
