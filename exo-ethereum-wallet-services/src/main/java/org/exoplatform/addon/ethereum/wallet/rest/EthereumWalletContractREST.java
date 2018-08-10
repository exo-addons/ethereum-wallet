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

  public static final Log LOG = ExoLogger.getLogger(EthereumWalletContractREST.class);

  private SettingService settingService;

  public EthereumWalletContractREST(SettingService settingService) {
    this.settingService = settingService;
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
    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    address= address.toLowerCase();
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT,
                                                                        WALLET_SCOPE,
                                                                        defaultContractsParamKey);
    String defaultContractsAddresses =
                                     defaultContractsAddressesValue == null ? address
                                                                            : defaultContractsAddressesValue.getValue().toString()
                                                                                + "," + address;
    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       defaultContractsParamKey,
                       SettingValue.create(defaultContractsAddresses));
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
    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    final String defaultAddressToSave = address.toLowerCase();
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT,
                                                                        WALLET_SCOPE,
                                                                        defaultContractsParamKey);
    if (defaultContractsAddressesValue != null) {
      String[] contractAddresses = defaultContractsAddressesValue.getValue().toString().split(",");
      Set<String> contractAddressList = Arrays.stream(contractAddresses)
                                               .filter(contractAddress -> !contractAddress.equalsIgnoreCase(defaultAddressToSave))
                                               .collect(Collectors.toSet());
      String contractAddressValue = StringUtils.join(contractAddressList, ",");
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey, SettingValue.create(contractAddressValue));
    }
    return Response.ok().build();
  }

  /**
   * Retrieves the list of default contract addreses
   * 
   * @param networkId
   * @return
   */
  public List<String> getContractAddresses(Long networkId) {
    if (networkId == null || networkId == 0) {
      return Collections.emptyList();
    }
    List<String> contractAddressList = null;
    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT,
                                                                        WALLET_SCOPE,
                                                                        defaultContractsParamKey);
    if (defaultContractsAddressesValue != null) {
      String[] contractAddresses = defaultContractsAddressesValue.getValue().toString().split(",");
      contractAddressList = Arrays.stream(contractAddresses).map(String::toLowerCase).collect(Collectors.toList());
    } else {
      contractAddressList = Collections.emptyList();
    }
    return contractAddressList;
  }
}
