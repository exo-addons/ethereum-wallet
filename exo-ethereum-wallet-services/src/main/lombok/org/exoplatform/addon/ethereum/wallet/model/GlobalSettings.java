package org.exoplatform.addon.ethereum.wallet.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.USER_ACCOUNT_TYPE;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.jsonArrayToList;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.json.*;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GlobalSettings implements Serializable {

  private static final long     serialVersionUID              = -4672745644323864680L;

  private Integer               dataVersion                   = 0;

  private boolean               walletEnabled                 = true;

  private boolean               enableDelegation              = true;

  private boolean               isAdmin                       = false;

  private String                accessPermission              = null;

  private String                fundsHolder                   = null;

  private String                initialFundsRequestMessage    = null;

  private String                fundsHolderType               = USER_ACCOUNT_TYPE;

  private String                providerURL                   = null;

  private String                websocketProviderURL          = null;

  private Long                  defaultNetworkId              = 0L;

  private Long                  defaultGas                    = 0L;

  private Long                  minGasPrice                   = 4000000000L;

  private Long                  normalGasPrice                = 8000000000L;

  private Long                  maxGasPrice                   = 15000000000L;

  private String                principalContractAdminName    = "Admin";

  private String                principalContractAdminAddress = null;

  private String                defaultPrincipalAccount       = null;

  private List<String>          defaultOverviewAccounts       = null;

  private Map<String, Double>   initialFunds;

  private UserPreferences       userPreferences;

  /**
   * Managed in other storage location
   */
  private transient Set<String> defaultContractsToDisplay;

  /**
   * Managed by code
   */
  private transient JSONArray   contractAbi                   = null;

  /**
   * Managed by code
   */
  private transient String      contractBin                   = null;

  public String toJSONString(boolean includeTransient) {
    return toJSONObject(includeTransient).toString();
  }

  public JSONObject toJSONObject(boolean includeTransient) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("enableDelegation", enableDelegation);
      if (StringUtils.isNotBlank(accessPermission)) {
        jsonObject.put("accessPermission", accessPermission);
      }
      if (StringUtils.isNotBlank(initialFundsRequestMessage)) {
        jsonObject.put("initialFundsRequestMessage", initialFundsRequestMessage);
      }
      if (StringUtils.isNotBlank(fundsHolder)) {
        jsonObject.put("fundsHolder", fundsHolder);
      }
      if (StringUtils.isNotBlank(fundsHolderType)) {
        jsonObject.put("fundsHolderType", fundsHolderType);
      }
      if (StringUtils.isNotBlank(providerURL)) {
        jsonObject.put("providerURL", providerURL);
      }
      if (StringUtils.isNotBlank(websocketProviderURL)) {
        jsonObject.put("websocketProviderURL", websocketProviderURL);
      }
      if (defaultNetworkId != null && defaultNetworkId != 0) {
        jsonObject.put("defaultNetworkId", defaultNetworkId);
      }
      if (defaultGas != null && defaultGas != 0) {
        jsonObject.put("defaultGas", defaultGas);
      }
      if (minGasPrice != null && minGasPrice != 0) {
        jsonObject.put("minGasPrice", minGasPrice);
      }
      if (normalGasPrice != null && normalGasPrice != 0) {
        jsonObject.put("normalGasPrice", normalGasPrice);
      }
      if (maxGasPrice != null && maxGasPrice != 0) {
        jsonObject.put("maxGasPrice", maxGasPrice);
      }
      if (dataVersion != null && dataVersion != 0) {
        jsonObject.put("dataVersion", dataVersion);
      }
      if (initialFunds != null && !initialFunds.isEmpty()) {
        JSONArray array = new JSONArray();
        Set<String> addresses = initialFunds.keySet();
        for (String address : addresses) {
          JSONObject obj = new JSONObject();
          obj.put("address", address);
          obj.put("amount", initialFunds.get(address));
          array.put(obj);
        }
        jsonObject.put("initialFunds", array);
      }
      if (StringUtils.isNotBlank(defaultPrincipalAccount)) {
        jsonObject.put("defaultPrincipalAccount", defaultPrincipalAccount);
      }
      if (defaultOverviewAccounts != null && !defaultOverviewAccounts.isEmpty()) {
        jsonObject.put("defaultOverviewAccounts", new JSONArray(defaultOverviewAccounts));
      }
      if (includeTransient) {
        jsonObject.put("isAdmin", isAdmin);
        jsonObject.put("isWalletEnabled", walletEnabled);
        if (userPreferences != null) {
          jsonObject.put("userPreferences", userPreferences.toJSONObject());
        }
        if (StringUtils.isNotBlank(principalContractAdminName)) {
          jsonObject.put("principalContractAdminName", principalContractAdminName);
        }
        if (StringUtils.isNotBlank(principalContractAdminAddress)) {
          jsonObject.put("principalContractAdminAddress", principalContractAdminAddress);
        }
        if (defaultContractsToDisplay != null && !defaultContractsToDisplay.isEmpty()) {
          jsonObject.put("defaultContractsToDisplay", new JSONArray(defaultContractsToDisplay));
        }
        if (contractAbi != null && contractAbi.length() > 0) {
          jsonObject.put("contractAbi", contractAbi);
        }
        if (StringUtils.isNotBlank(contractBin)) {
          jsonObject.put("contractBin", contractBin);
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    return toJSONString(false);
  }

  public static final GlobalSettings parseStringToObject(GlobalSettings defaultSettings, String jsonString) {
    if (defaultSettings == null) {
      defaultSettings = new GlobalSettings();
    }
    if (StringUtils.isBlank(jsonString)) {
      try {
        return (GlobalSettings) defaultSettings.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      GlobalSettings globalSettings = new GlobalSettings();

      String storedFundsHolder = jsonObject.has("fundsHolder") ? jsonObject.getString("fundsHolder")
                                                               : defaultSettings.getFundsHolder();
      globalSettings.setFundsHolder(storedFundsHolder);

      String storedFundsHolderType = jsonObject.has("fundsHolderType") ? jsonObject.getString("fundsHolderType")
                                                                       : defaultSettings.getFundsHolderType();
      globalSettings.setFundsHolderType(storedFundsHolderType);

      Map<String, Double> storedInitialFunds = jsonObject.has("initialFunds") ? toMap(jsonObject.getJSONArray("initialFunds"))
                                                                              : defaultSettings.getInitialFunds();
      globalSettings.setInitialFunds(storedInitialFunds);

      String storedAccessPermission = jsonObject.has("accessPermission") ? jsonObject.getString("accessPermission")
                                                                         : defaultSettings.getAccessPermission();
      globalSettings.setAccessPermission(storedAccessPermission);

      String storedInitialfundsRequestMessage =
                                              jsonObject.has("initialFundsRequestMessage") ? jsonObject.getString("initialFundsRequestMessage")
                                                                                           : defaultSettings.getInitialFundsRequestMessage();
      globalSettings.setInitialFundsRequestMessage(storedInitialfundsRequestMessage);

      String storedProviderURL = jsonObject.has("providerURL") ? jsonObject.getString("providerURL")
                                                               : defaultSettings.getProviderURL();
      globalSettings.setProviderURL(storedProviderURL);

      String storedWebsocketProviderURL = jsonObject.has("websocketProviderURL") ? jsonObject.getString("websocketProviderURL")
                                                                                 : defaultSettings.getWebsocketProviderURL();
      globalSettings.setWebsocketProviderURL(storedWebsocketProviderURL);

      long storedDefaultNetworkId = jsonObject.has("defaultNetworkId") ? jsonObject.getLong("defaultNetworkId")
                                                                       : defaultSettings.getDefaultNetworkId();
      globalSettings.setDefaultNetworkId(storedDefaultNetworkId);

      long storedDefaultGas = jsonObject.has("defaultGas") ? jsonObject.getLong("defaultGas") : defaultSettings.getDefaultGas();
      globalSettings.setDefaultGas(storedDefaultGas);

      long storedMinGasPrice =
                             jsonObject.has("minGasPrice") ? jsonObject.getLong("minGasPrice") : defaultSettings.getMinGasPrice();
      globalSettings.setMinGasPrice(storedMinGasPrice);

      long storedNormalGasPrice = jsonObject.has("normalGasPrice") ? jsonObject.getLong("normalGasPrice")
                                                                   : defaultSettings.getNormalGasPrice();
      globalSettings.setNormalGasPrice(storedNormalGasPrice);

      long storedMaxGasPrice =
                             jsonObject.has("maxGasPrice") ? jsonObject.getLong("maxGasPrice") : defaultSettings.getMaxGasPrice();
      globalSettings.setMaxGasPrice(storedMaxGasPrice);

      boolean storedEnableDelegation = jsonObject.has("enableDelegation") ? jsonObject.getBoolean("enableDelegation")
                                                                          : defaultSettings.isEnableDelegation();
      globalSettings.setEnableDelegation(storedEnableDelegation);

      String storedDefaultPrincipalAccount =
                                           jsonObject.has("defaultPrincipalAccount") ? jsonObject.getString("defaultPrincipalAccount")
                                                                                     : defaultSettings.getDefaultPrincipalAccount();
      globalSettings.setDefaultPrincipalAccount(storedDefaultPrincipalAccount);

      globalSettings.setDefaultOverviewAccounts(jsonArrayToList(jsonObject, "defaultOverviewAccounts"));

      globalSettings.setDataVersion(jsonObject.has("dataVersion") ? jsonObject.getInt("dataVersion") : 0);
      return globalSettings;
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting JSON String to Object", e);
    }
  }

  public static final GlobalSettings parseStringToObject(String jsonString) {
    return parseStringToObject(null, jsonString);
  }

  private static Map<String, Double> toMap(JSONArray storedInitialFunds) throws JSONException {
    HashMap<String, Double> map = new HashMap<>();
    if (storedInitialFunds == null || storedInitialFunds.length() == 0) {
      return map;
    }
    for (int i = 0; i < storedInitialFunds.length(); i++) {
      JSONObject obj = storedInitialFunds.getJSONObject(i);
      map.put(obj.getString("address"), obj.getDouble("amount"));
    }
    return map;
  }

}
