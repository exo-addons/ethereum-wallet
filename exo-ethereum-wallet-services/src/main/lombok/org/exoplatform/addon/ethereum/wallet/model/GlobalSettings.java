package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.*;

import lombok.Data;

@Data
public class GlobalSettings implements Serializable {

  private static final long      serialVersionUID        = -4672745644323864680L;

  private boolean                walletEnabled           = true;

  private String                 accessPermission        = null;

  private String                 providerURL             = "https://ropsten.infura.io";

  private Integer                defaultBlocksToRetrieve = 100;

  private Long                   defaultNetworkId        = 3L;

  private Integer                defaultGas              = 65000;

  private Integer                userDefaultGas;

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
      jsonObject.put("accessPermission", accessPermission);
      jsonObject.put("providerURL", providerURL);
      jsonObject.put("defaultBlocksToRetrieve", defaultBlocksToRetrieve);
      jsonObject.put("defaultNetworkId", defaultNetworkId);
      jsonObject.put("defaultGas", defaultGas);
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

      String storedAccessPermission = jsonObject.getString("accessPermission");
      globalSettings.setAccessPermission(storedAccessPermission == null
          || storedAccessPermission.isEmpty() ? defaultSettings.getAccessPermission() : storedAccessPermission);

      String storedProviderURL = jsonObject.getString("providerURL");
      globalSettings.setProviderURL(storedProviderURL == null || storedProviderURL.isEmpty() ? defaultSettings.getProviderURL()
                                                                                             : storedProviderURL);

      int storedDefaultBlocksToRetrieve = jsonObject.getInt("defaultBlocksToRetrieve");
      globalSettings.setDefaultBlocksToRetrieve(storedDefaultBlocksToRetrieve == 0 ? defaultSettings.getDefaultBlocksToRetrieve()
                                                                                   : storedDefaultBlocksToRetrieve);

      long storedDefaultNetworkId = jsonObject.getLong("defaultNetworkId");
      globalSettings.setDefaultNetworkId(storedDefaultNetworkId == 0L ? defaultSettings.getDefaultBlocksToRetrieve()
                                                                      : storedDefaultNetworkId);

      int storedDefaultGas = jsonObject.getInt("defaultGas");
      globalSettings.setDefaultGas(storedDefaultGas == 0 ? defaultSettings.getDefaultGas() : storedDefaultGas);

      return globalSettings;
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting JSON String to Object", e);
    }
  }

  public static final GlobalSettings parseStringToObject(String jsonString) {
    return parseStringToObject(null, jsonString);
  }
}
