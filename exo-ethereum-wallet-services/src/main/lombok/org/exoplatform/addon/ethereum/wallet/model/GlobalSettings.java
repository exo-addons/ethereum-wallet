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

  private Integer               defaultBlocksToRetrieve       = 0;

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
      jsonObject.put("isWalletEnabled", walletEnabled);
      jsonObject.put("isAdmin", isAdmin);
      jsonObject.put("enableDelegation", enableDelegation);
      jsonObject.put("accessPermission", accessPermission);
      jsonObject.put("initialFundsRequestMessage", initialFundsRequestMessage);
      jsonObject.put("fundsHolder", fundsHolder);
      jsonObject.put("fundsHolderType", fundsHolderType);
      jsonObject.put("providerURL", providerURL);
      jsonObject.put("websocketProviderURL", websocketProviderURL);
      jsonObject.put("defaultBlocksToRetrieve", defaultBlocksToRetrieve);
      jsonObject.put("defaultNetworkId", defaultNetworkId);
      jsonObject.put("defaultGas", defaultGas);
      jsonObject.put("minGasPrice", minGasPrice);
      jsonObject.put("normalGasPrice", normalGasPrice);
      jsonObject.put("maxGasPrice", maxGasPrice);
      jsonObject.put("dataVersion", dataVersion);

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
      if (userPreferences != null) {
        jsonObject.put("userPreferences", userPreferences.toJSONObject());
      }
      if (defaultPrincipalAccount != null) {
        jsonObject.put("defaultPrincipalAccount", defaultPrincipalAccount);
      }
      if (defaultOverviewAccounts != null) {
        jsonObject.put("defaultOverviewAccounts", new JSONArray(defaultOverviewAccounts));
      }
      if (includeTransient) {
        if (principalContractAdminName != null) {
          jsonObject.put("principalContractAdminName", principalContractAdminName);
        }
        if (principalContractAdminAddress != null) {
          jsonObject.put("principalContractAdminAddress", principalContractAdminAddress);
        }
        if (defaultContractsToDisplay != null) {
          jsonObject.put("defaultContractsToDisplay", new JSONArray(defaultContractsToDisplay));
        }
        if (contractAbi != null) {
          jsonObject.put("contractAbi", contractAbi);
        }
        if (contractBin != null) {
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
      String storedFundsHolder = jsonObject.has("fundsHolder") ? jsonObject.getString("fundsHolder") : null;
      globalSettings.setFundsHolder(storedFundsHolder == null || storedFundsHolder.isEmpty() ? defaultSettings.getFundsHolder()
                                                                                             : storedFundsHolder);
      String storedFundsHolderType = jsonObject.has("fundsHolderType") ? jsonObject.getString("fundsHolderType") : null;
      globalSettings.setFundsHolderType(storedFundsHolderType == null
          || storedFundsHolderType.isEmpty() ? defaultSettings.getFundsHolderType() : storedFundsHolderType);
      JSONArray storedInitialFunds = jsonObject.has("initialFunds") ? jsonObject.getJSONArray("initialFunds") : null;
      globalSettings.setInitialFunds(storedInitialFunds == null ? defaultSettings.getInitialFunds() : toMap(storedInitialFunds));
      String storedAccessPermission = jsonObject.has("accessPermission") ? jsonObject.getString("accessPermission") : null;
      globalSettings.setAccessPermission(storedAccessPermission == null
          || storedAccessPermission.isEmpty() ? defaultSettings.getAccessPermission() : storedAccessPermission);
      String storedInitialfundsRequestMessage =
                                              jsonObject.has("initialFundsRequestMessage") ? jsonObject.getString("initialFundsRequestMessage")
                                                                                           : null;
      globalSettings.setInitialFundsRequestMessage(storedInitialfundsRequestMessage == null
          || storedInitialfundsRequestMessage.isEmpty() ? defaultSettings.getInitialFundsRequestMessage()
                                                        : storedInitialfundsRequestMessage);
      String storedProviderURL = jsonObject.has("providerURL") ? jsonObject.getString("providerURL") : null;
      globalSettings.setProviderURL(storedProviderURL);
      String storedWebsocketProviderURL = jsonObject.has("websocketProviderURL") ? jsonObject.getString("websocketProviderURL")
                                                                                 : null;
      globalSettings.setWebsocketProviderURL(storedWebsocketProviderURL);
      int storedDefaultBlocksToRetrieve = jsonObject.has("defaultBlocksToRetrieve") ? jsonObject.getInt("defaultBlocksToRetrieve")
                                                                                    : 0;
      globalSettings.setDefaultBlocksToRetrieve(storedDefaultBlocksToRetrieve == 0 ? defaultSettings.getDefaultBlocksToRetrieve()
                                                                                   : storedDefaultBlocksToRetrieve);
      long storedDefaultNetworkId = jsonObject.has("defaultNetworkId") ? jsonObject.getLong("defaultNetworkId") : 0;
      globalSettings.setDefaultNetworkId(storedDefaultNetworkId == 0L ? defaultSettings.getDefaultBlocksToRetrieve()
                                                                      : storedDefaultNetworkId);
      long storedDefaultGas = jsonObject.has("defaultGas") ? jsonObject.getLong("defaultGas") : 0;
      globalSettings.setDefaultGas(storedDefaultGas == 0 ? defaultSettings.getDefaultGas() : storedDefaultGas);
      long storedMinGasPrice = jsonObject.has("minGasPrice") ? jsonObject.getLong("minGasPrice") : 0;
      globalSettings.setMinGasPrice(storedMinGasPrice == 0 ? defaultSettings.getMinGasPrice() : storedMinGasPrice);
      long storedNormalGasPrice = jsonObject.has("normalGasPrice") ? jsonObject.getLong("normalGasPrice") : 0;
      globalSettings.setNormalGasPrice(storedNormalGasPrice == 0 ? defaultSettings.getNormalGasPrice() : storedNormalGasPrice);
      long storedMaxGasPrice = jsonObject.has("maxGasPrice") ? jsonObject.getLong("maxGasPrice") : 0;
      globalSettings.setMaxGasPrice(storedMaxGasPrice == 0 ? defaultSettings.getMaxGasPrice() : storedMaxGasPrice);
      boolean storedEnableDelegation = jsonObject.has("enableDelegation") ? jsonObject.getBoolean("enableDelegation") : true;
      globalSettings.setEnableDelegation(storedEnableDelegation);
      String storedDefaultPrincipalAccount =
                                           jsonObject.has("defaultPrincipalAccount") ? jsonObject.getString("defaultPrincipalAccount")
                                                                                     : null;
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
