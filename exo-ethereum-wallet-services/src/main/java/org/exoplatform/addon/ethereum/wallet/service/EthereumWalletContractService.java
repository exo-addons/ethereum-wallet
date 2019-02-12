package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.utils.Utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.picocontainer.Startable;

import org.exoplatform.addon.ethereum.wallet.model.ContractDetail;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumWalletContractService implements Startable {

  private static final Log     LOG                                    =
                                   ExoLogger.getLogger(EthereumWalletContractService.class);

  private static final String  ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE =
                                                                      "address parameter is mandatory";

  private ConfigurationManager configurationManager;

  private String               contractAbiPath;

  private JSONArray            contractAbi;

  private String               contractBinaryPath;

  private String               contractBinary;

  private SettingService       settingService;

  public EthereumWalletContractService(ConfigurationManager configurationManager,
                                       SettingService settingService,
                                       InitParams params) {
    this.settingService = settingService;
    this.configurationManager = configurationManager;

    if (params.containsKey(ABI_PATH_PARAMETER)) {
      contractAbiPath = params.getValueParam(ABI_PATH_PARAMETER).getValue();
    }
    if (StringUtils.isBlank(contractAbiPath)) {
      LOG.warn("Contract ABI path is empty, thus no contract deployment is possible");
    }
    if (params.containsKey(BIN_PATH_PARAMETER)) {
      contractBinaryPath = params.getValueParam(BIN_PATH_PARAMETER).getValue();
    }
    if (StringUtils.isBlank(contractBinaryPath)) {
      LOG.warn("Contract BIN path is empty, thus no contract deployment is possible");
    }
  }

  @Override
  public void start() {
    try {
      String contractAbiString = IOUtil.getStreamContentAsString(this.configurationManager.getInputStream(contractAbiPath));
      contractAbi = new JSONArray(contractAbiString);
      contractBinary = IOUtil.getStreamContentAsString(this.configurationManager.getInputStream(contractBinaryPath));
      if (!contractBinary.startsWith("0x")) {
        contractBinary = "0x" + contractBinary;
      }
    } catch (Exception e) {
      LOG.error("Can't read ABI/BIN files content", e);
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  /**
   * @param address contract address to check
   * @param networkId blockchain network id
   * @return true if contract address is a watched contract
   */
  public boolean isContract(String address, long networkId) {
    return getContractDetail(address, networkId) != null;
  }

  /**
   * Save a new contract details
   * 
   * @param contractDetail contract details to save
   */
  public void saveContract(ContractDetail contractDetail) {
    if (StringUtils.isBlank(contractDetail.getAddress())) {
      throw new IllegalArgumentException(ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE);
    }
    if (contractDetail.getNetworkId() == null || contractDetail.getNetworkId() == 0) {
      throw new IllegalArgumentException("networkId parameter is mandatory");
    }

    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + contractDetail.getNetworkId();

    String address = contractDetail.getAddress().toLowerCase();

    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       address + contractDetail.getNetworkId(),
                       SettingValue.create(contractDetail.toJSONString()));

    if (contractDetail.isDefaultContract()) {
      // Save the contract address in the list of default contract addreses
      SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey);
      String defaultContractsAddresses = defaultContractsAddressesValue == null ? address
                                                                                : defaultContractsAddressesValue.getValue()
                                                                                                                .toString()
                                                                                    + "," + address;
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey, SettingValue.create(defaultContractsAddresses));
    }
  }

  /**
   * Removes a contract address from default contracts displayed in wallet of
   * all users
   * 
   * @param address contract address to remove from watched list
   * @param networkId blockchain network id where contract is deployed
   * @return true if removed
   */
  public boolean removeDefaultContract(String address, Long networkId) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Can't remove empty address for contract");
      return false;
    }
    if (networkId == null || networkId == 0) {
      LOG.warn("Can't remove empty network id for contract");
      return false;
    }

    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    final String defaultAddressToSave = address.toLowerCase();
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey);
    if (defaultContractsAddressesValue != null) {
      String[] contractAddresses = defaultContractsAddressesValue.getValue().toString().split(",");
      Set<String> contractAddressList = Arrays.stream(contractAddresses)
                                              .filter(contractAddress -> !contractAddress.equalsIgnoreCase(defaultAddressToSave))
                                              .collect(Collectors.toSet());
      String contractAddressValue = StringUtils.join(contractAddressList, ",");

      settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, address + networkId);
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey, SettingValue.create(contractAddressValue));
    }
    return true;
  }

  /**
   * Get contract detail
   * 
   * @param address contract address to get from watched list
   * @param networkId blockchain network id where contract is deployed
   * @return {@link ContractDetail} contract details
   */
  public ContractDetail getContractDetail(String address, Long networkId) {
    if (StringUtils.isBlank(address)) {
      return null;
    }

    Set<String> defaultContracts = getDefaultContractsAddresses(networkId);
    if (defaultContracts != null && !defaultContracts.contains(address)) {
      return null;
    }

    SettingValue<?> contractDetailValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, address + networkId);
    if (contractDetailValue != null) {
      return ContractDetail.parseStringToObject((String) contractDetailValue.getValue());
    }
    return null;
  }

  /**
   * Retrieves the list of default contract addreses
   * 
   * @param networkId blockchain network id where contract is deployed
   * @return {@link Set} of watched contracts addresses
   */
  public Set<String> getDefaultContractsAddresses(Long networkId) {
    if (networkId == null || networkId == 0) {
      return Collections.emptySet();
    }

    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    SettingValue<?> defaultContractsAddressesValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey);
    if (defaultContractsAddressesValue != null) {
      String defaultContractsAddressesString = defaultContractsAddressesValue.getValue().toString().toLowerCase();
      String[] contractAddresses = defaultContractsAddressesString.split(",");
      return Arrays.stream(contractAddresses).map(String::toLowerCase).collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  /**
   * Retreive the ABI content of a contract
   * 
   * @param name contract name
   * @param extension contract ABI file extension ('json' or 'abi')
   * @return ABI of contract in JSON format represented in {@link String}
   * @throws IOException when an error occurs while getting contract ABI file from filesystem
   */
  public String getContract(String name, String extension) throws IOException {
    try (InputStream abiInputStream = this.getClass()
                                          .getClassLoader()
                                          .getResourceAsStream("org/exoplatform/addon/ethereum/wallet/contract/" + name + "."
                                              + extension)) {
      return IOUtils.toString(abiInputStream);
    }
  }

  /**
   * Get Contract ABI
   * 
   * @return {@link JSONArray} ABI of contract in JSON format
   */
  public JSONArray getContractAbi() {
    return contractAbi;
  }

  /**
   * Get Contract BINARY to deploy
   * 
   * @return UTF-8 String of contract BIN
   */
  public String getContractBinary() {
    return contractBinary;
  }

}
