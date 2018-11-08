package org.exoplatform.addon.ethereum.wallet.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.jsonArrayToList;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.*;

import lombok.Data;

@Data
public class UserPreferences implements Serializable {

  private static final long serialVersionUID = -5725443183560646198L;

  private Integer           dataVersion      = 0;

  private Integer           defaultGas       = 0;

  private String            currency         = "usd";

  private String            walletAddress    = null;

  private String            phrase           = null;

  private String            principalAccount = null;

  private List<String>      overviewAccounts = null;

  private Boolean           enableDelegation = null;

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("currency", currency);
      jsonObject.put("defaultGas", defaultGas);
      jsonObject.put("walletAddress", walletAddress);
      jsonObject.put("phrase", phrase);
      jsonObject.put("dataVersion", dataVersion);
      if (enableDelegation != null) {
        jsonObject.put("enableDelegation", enableDelegation);
      }
      if (principalAccount != null) {
        jsonObject.put("principalAccount", principalAccount);
      }
      if (overviewAccounts != null) {
        jsonObject.put("overviewAccounts", new JSONArray(overviewAccounts));
      }
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
      if (jsonObject.has("phrase")) {
        userPreferences.setWalletAddress(jsonObject.getString("phrase"));
      }
      if (jsonObject.has("principalAccount")) {
        userPreferences.setPrincipalAccount(jsonObject.getString("principalAccount"));
      }
      if (jsonObject.has("enableDelegation")) {
        userPreferences.setEnableDelegation(jsonObject.getBoolean("enableDelegation"));
      }
      if (jsonObject.has("dataVersion")) {
        userPreferences.setDataVersion(jsonObject.getInt("dataVersion"));
      }
      userPreferences.setOverviewAccounts(jsonArrayToList(jsonObject, "overviewAccounts"));
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
