package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.*;

import lombok.Data;

@Data
public class GlobalSettings implements Serializable {

  private static final long      serialVersionUID        = -4672745644323864680L;

  private String                 accessPermission;

  private String                 providerURL;

  private Integer                defaultBlocksToRetrieve = 1000;

  private Long                   defaultNetworkId        = 0L;

  private Integer                defaultGas              = 35000;

  private Integer                userDefaultGas          = 35000;

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

  public static final GlobalSettings parseStringToObject(String jsonString) {
    if (StringUtils.isBlank(jsonString)) {
      return null;
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      GlobalSettings globaalSettings = new GlobalSettings();
      globaalSettings.setAccessPermission(jsonObject.getString("accessPermission"));
      globaalSettings.setProviderURL(jsonObject.getString("providerURL"));
      globaalSettings.setDefaultBlocksToRetrieve(jsonObject.getInt("defaultBlocksToRetrieve"));
      globaalSettings.setDefaultNetworkId(jsonObject.getLong("defaultNetworkId"));
      globaalSettings.setDefaultGas(jsonObject.getInt("defaultGas"));
      return globaalSettings;
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting JSON String to Object", e);
    }
  }
}
