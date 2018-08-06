package org.exoplatform.addon.ethereum.wallet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/delete a contract as default displayed
 * contracts for end users
 */
@Path("/wallet/api/contract")
@RolesAllowed("administrators")
public class EthereumWalletContractREST implements ResourceContainer {

  public static final String  WALLET_DEFAULT_CONTRACTS_NAME = "WALLET_DEFAULT_CONTRACTS";

  public static final String  SCOPE_NAME                    = "ADDONS_ETHEREUM_WALLET";

  public static final Context WALLET_CONTEXT                = Context.GLOBAL;

  public static final Scope   WALLET_SCOPE                  = Scope.APPLICATION.id(SCOPE_NAME);

  private SettingService      settingService;

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
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT,
                                                                        WALLET_SCOPE,
                                                                        WALLET_DEFAULT_CONTRACTS_NAME);
    if (defaultContractsAddressesValue != null) {
      String[] contractAddresses = defaultContractsAddressesValue.getValue().toString().split(",");
      List<String> contractAddressList = Arrays.stream(contractAddresses).collect(Collectors.toList());
      return Response.ok(contractAddressList).build();
    }
    return Response.ok("{}").build();
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
      List<String> contractAddressList = Arrays.stream(contractAddresses)
                                               .filter(contractAddress -> contractAddress.equals(address))
                                               .collect(Collectors.toList());
      String contractAddressValue = StringUtils.join(contractAddressList, ",");
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, WALLET_DEFAULT_CONTRACTS_NAME, SettingValue.create(contractAddressValue));
    }
    return Response.ok().build();
  }
}
