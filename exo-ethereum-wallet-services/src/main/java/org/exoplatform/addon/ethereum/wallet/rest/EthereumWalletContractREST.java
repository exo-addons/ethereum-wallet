package org.exoplatform.addon.ethereum.wallet.rest;

import static org.exoplatform.addon.ethereum.wallet.rest.Utils.*;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/delete a contract as default
 * displayed contracts for end users
 */
@Path("/wallet/api/contract")
@RolesAllowed("administrators")
public class EthereumWalletContractREST implements ResourceContainer {

  private SettingService settingService;

  public EthereumWalletContractREST(SettingService settingService) {
    this.settingService = settingService;
  }

  /**
   * Retrieves the list of default contracts displayed in all users wallet
   * 
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getContracts() {
    List<String> contractAddressList = getContractAddresses();
    if (contractAddressList == null) {
      return Response.ok("{}").build();
    } else {
      return Response.ok(new JSONArray(contractAddressList).toString()).build();
    }
  }

  /**
   * Save a new contract address to display it in wallet of all users
   * 
   * @param address
   * @return
   */
  @POST
  @Path("save")
  public Response saveContract(@FormParam("address") String address) {
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT,
                                                                        WALLET_SCOPE,
                                                                        WALLET_DEFAULT_CONTRACTS_NAME);
    String defaultContractsAddresses =
                                     defaultContractsAddressesValue == null ? address
                                                                            : defaultContractsAddressesValue.getValue().toString()
                                                                                + "," + address;
    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       WALLET_DEFAULT_CONTRACTS_NAME,
                       SettingValue.create(defaultContractsAddresses));
    return Response.ok().build();
  }

  /**
   * Removes a contract address from default contracts displayed in wallet of
   * all users
   * 
   * @param address
   * @return
   */
  @POST
  @Path("remove")
  public Response removeContract(@FormParam("address") String address) {
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT,
                                                                        WALLET_SCOPE,
                                                                        WALLET_DEFAULT_CONTRACTS_NAME);
    if (defaultContractsAddressesValue != null) {
      String[] contractAddresses = defaultContractsAddressesValue.getValue().toString().split(",");
      Set<String> contractAddressList = Arrays.stream(contractAddresses)
                                               .filter(contractAddress -> !contractAddress.equalsIgnoreCase(address))
                                               .collect(Collectors.toSet());
      String contractAddressValue = StringUtils.join(contractAddressList, ",");
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, WALLET_DEFAULT_CONTRACTS_NAME, SettingValue.create(contractAddressValue));
    }
    return Response.ok().build();
  }

  /**
   * Retrieves the list of default contract addreses
   * 
   * @return
   */
  public List<String> getContractAddresses() {
    List<String> contractAddressList = null;
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT,
                                                                        WALLET_SCOPE,
                                                                        WALLET_DEFAULT_CONTRACTS_NAME);
    if (defaultContractsAddressesValue != null) {
      String[] contractAddresses = defaultContractsAddressesValue.getValue().toString().split(",");
      contractAddressList = Arrays.stream(contractAddresses).collect(Collectors.toList());
    } else {
      contractAddressList = Collections.emptyList();
    }
    return contractAddressList;
  }
}
