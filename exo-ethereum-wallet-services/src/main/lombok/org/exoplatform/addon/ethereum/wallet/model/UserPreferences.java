package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Data;

@Data
public class UserPreferences implements Serializable {

  private static final long serialVersionUID = -5725443183560646198L;

  private Integer           defaultGas       = 0;

  private String            currency         = "usd";

  private String            walletAddress    = null;

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("currency", currency);
      jsonObject.put("defaultGas", defaultGas);
      jsonObject.put("walletAddress", walletAddress);
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  public static final UserPreferences parseStringToObject(String jsonString) {
    if (StringUtils.isBlank(jsonString)) {
      return null;
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      UserPreferences userPreferences = new UserPreferences();
      if (jsonObject.has("currency")) {
        userPreferences.setCurrency(jsonObject.getString("currency"));
      }
      if (jsonObject.has("defaultGas")) {
        userPreferences.setDefaultGas(jsonObject.getInt("defaultGas"));
      }
      if (jsonObject.has("walletAddress")) {
        userPreferences.setWalletAddress(jsonObject.getString("walletAddress"));
      }
      return userPreferences;
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting JSON String to Object", e);
    }
  }

  @Override
  public String toString() {
    return toJSONString();
  }
}
