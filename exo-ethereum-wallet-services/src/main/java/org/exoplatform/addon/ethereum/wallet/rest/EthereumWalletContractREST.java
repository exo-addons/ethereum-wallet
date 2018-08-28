package org.exoplatform.addon.ethereum.wallet.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletStorage;
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

  private EthereumWalletStorage ethereumWalletStorage;

  public EthereumWalletContractREST(EthereumWalletStorage ethereumWalletStorage) {
    this.ethereumWalletStorage = ethereumWalletStorage;
  }

  /**
   * Save a new contract address to display it in wallet of all users
   * 
   * @param address
   * @param networkId
   * @return
   */
  @POST
  @Path("save")
  public Response saveContract(@FormParam("address") String address, @FormParam("networkId") Long networkId) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Can't save empty address for contract");
      return Response.status(400).build();
    }
    if (networkId == null || networkId == 0) {
      LOG.warn("Can't remove empty network id for contract");
      return Response.status(400).build();
    }
    ethereumWalletStorage.saveDefaultContract(address, networkId);
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
    ethereumWalletStorage.removeDefaultContract(address, networkId);
    return Response.ok().build();
  }
}
