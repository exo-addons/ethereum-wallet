package org.exoplatform.addon.ethereum.wallet.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.USER_ACCOUNT_TYPE;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.jsonArrayToList;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.json.*;

import lombok.Data;

@Data
public class GlobalSettings implements Serializable {

  private static final long      serialVersionUID           = -4672745644323864680L;

  private boolean                walletEnabled              = true;

  private boolean                enableDelegation           = true;

  private String                 accessPermission           = null;

  private String                 fundsHolder                = null;

  private String                 initialfundsRequestMessage = null;

  private String                 fundsHolderType            = USER_ACCOUNT_TYPE;

  private String                 providerURL                = "https://ropsten.infura.io";

  private String                 websocketProviderURL       = "wss://ropsten.infura.io/ws";

  private Integer                defaultBlocksToRetrieve    = 100;

  private Long                   defaultNetworkId           = 3L;

  private Integer                defaultGas                 = 65000;

  private String                 defaultPrincipalAccount    = null;

  private List<String>           defaultOverviewAccounts    = null;

  private Map<String, Double>    initialFunds;

  private UserPreferences        userPreferences;

  /**
   * Managed in other storage location
   */
  private transient List<String> defaultContractsToDisplay;

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("isWalletEnabled", walletEnabled);
      jsonObject.put("enableDelegation", enableDelegation);
      jsonObject.put("accessPermission", accessPermission);
      jsonObject.put("initialfundsRequestMessage", initialfundsRequestMessage);
      jsonObject.put("fundsHolder", fundsHolder);
      jsonObject.put("fundsHolderType", fundsHolderType);
      jsonObject.put("providerURL", providerURL);
      jsonObject.put("websocketProviderURL", websocketProviderURL);
      jsonObject.put("defaultBlocksToRetrieve", defaultBlocksToRetrieve);
      jsonObject.put("defaultNetworkId", defaultNetworkId);
      jsonObject.put("defaultGas", defaultGas);
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

      jsonObject.put("defaultContractsToDisplay", new JSONArray(defaultContractsToDisplay));
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    return toJSONString();
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
                                              jsonObject.has("initialfundsRequestMessage") ? jsonObject.getString("initialfundsRequestMessage")
                                                                                           : null;
      globalSettings.setInitialfundsRequestMessage(storedInitialfundsRequestMessage == null
          || storedInitialfundsRequestMessage.isEmpty() ? defaultSettings.getInitialfundsRequestMessage()
                                                        : storedInitialfundsRequestMessage);

      String storedProviderURL = jsonObject.has("providerURL") ? jsonObject.getString("providerURL") : null;
      globalSettings.setProviderURL(storedProviderURL == null || storedProviderURL.isEmpty() ? defaultSettings.getProviderURL()
                                                                                             : storedProviderURL);

      String storedWebsocketProviderURL = jsonObject.has("websocketProviderURL") ? jsonObject.getString("websocketProviderURL")
                                                                                 : null;
      globalSettings.setWebsocketProviderURL(storedWebsocketProviderURL == null
          || storedWebsocketProviderURL.isEmpty() ? defaultSettings.getProviderURL() : storedWebsocketProviderURL);

      int storedDefaultBlocksToRetrieve = jsonObject.has("defaultBlocksToRetrieve") ? jsonObject.getInt("defaultBlocksToRetrieve")
                                                                                    : 0;
      globalSettings.setDefaultBlocksToRetrieve(storedDefaultBlocksToRetrieve == 0 ? defaultSettings.getDefaultBlocksToRetrieve()
                                                                                   : storedDefaultBlocksToRetrieve);

      long storedDefaultNetworkId = jsonObject.has("defaultNetworkId") ? jsonObject.getLong("defaultNetworkId") : 0;
      globalSettings.setDefaultNetworkId(storedDefaultNetworkId == 0L ? defaultSettings.getDefaultBlocksToRetrieve()
                                                                      : storedDefaultNetworkId);

      int storedDefaultGas = jsonObject.has("defaultGas") ? jsonObject.getInt("defaultGas") : 0;
      globalSettings.setDefaultGas(storedDefaultGas == 0 ? defaultSettings.getDefaultGas() : storedDefaultGas);

      boolean storedEnableDelegation = jsonObject.has("enableDelegation") ? jsonObject.getBoolean("enableDelegation") : true;
      globalSettings.setEnableDelegation(storedEnableDelegation);

      String storedDefaultPrincipalAccount =
                                           jsonObject.has("defaultPrincipalAccount") ? jsonObject.getString("defaultPrincipalAccount")
                                                                                     : null;
      globalSettings.setDefaultPrincipalAccount(storedDefaultPrincipalAccount);

      globalSettings.setDefaultOverviewAccounts(jsonArrayToList(jsonObject, "defaultOverviewAccounts"));

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
