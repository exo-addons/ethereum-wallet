package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Data;

@Data
public class UserPreferences implements Serializable {

  private static final long serialVersionUID = -5725443183560646198L;

  private Integer           defaultGas       = 35000;

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("defaultGas", defaultGas);
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
      userPreferences.setDefaultGas(jsonObject.getInt("defaultGas"));
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
