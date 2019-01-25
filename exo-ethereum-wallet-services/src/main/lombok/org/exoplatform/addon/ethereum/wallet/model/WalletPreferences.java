package org.exoplatform.addon.ethereum.wallet.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.jsonArrayToList;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.*;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WalletPreferences implements Serializable {

  private static final String OVERVIEW_ACCOUNTS_PARAM = "overviewAccounts";

  private static final String PRINCIPAL_ACCOUNT_PARAM = "principalAccount";

  private static final String ENABLE_DELEGATION_PARAM = "enableDelegation";

  private static final String DATA_VERSION_PARAM      = "dataVersion";

  private static final String PHRASE_PARAM            = "phrase";

  private static final String WALLET_ADDRESS_PARAM    = "walletAddress";

  private static final String DEFAULT_GAS_PARAM       = "defaultGas";

  private static final String CURRENCY_PARAM          = "currency";

  private static final long   serialVersionUID        = -5725443183560646198L;

  private Integer             dataVersion             = 0;

  private Long                defaultGas              = 0L;

  private String              currency                = "usd";

  private String              walletAddress           = null;

  private String              phrase                  = null;

  private String              principalAccount        = null;

  private Set<String>         overviewAccounts;

  private Boolean             enableDelegation        = null;

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put(CURRENCY_PARAM, currency);
      jsonObject.put(DEFAULT_GAS_PARAM, defaultGas);
      jsonObject.put(WALLET_ADDRESS_PARAM, walletAddress);
      jsonObject.put(PHRASE_PARAM, phrase);
      jsonObject.put(DATA_VERSION_PARAM, dataVersion);
      if (enableDelegation != null) {
        jsonObject.put(ENABLE_DELEGATION_PARAM, enableDelegation);
      }
      if (principalAccount != null) {
        jsonObject.put(PRINCIPAL_ACCOUNT_PARAM, principalAccount);
      }
      if (overviewAccounts != null) {
        jsonObject.put(OVERVIEW_ACCOUNTS_PARAM, new JSONArray(overviewAccounts));
      }
    } catch (JSONException e) {
      throw new IllegalStateException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  public static final WalletPreferences parseStringToObject(String jsonString) {
    if (StringUtils.isBlank(jsonString)) {
      return null;
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      WalletPreferences userPreferences = new WalletPreferences();
      if (jsonObject.has(CURRENCY_PARAM)) {
        userPreferences.setCurrency(jsonObject.getString(CURRENCY_PARAM));
      }
      if (jsonObject.has(DEFAULT_GAS_PARAM)) {
        userPreferences.setDefaultGas(jsonObject.getLong(DEFAULT_GAS_PARAM));
      }
      if (jsonObject.has(WALLET_ADDRESS_PARAM)) {
        userPreferences.setWalletAddress(jsonObject.getString(WALLET_ADDRESS_PARAM));
      }
      if (jsonObject.has(PHRASE_PARAM)) {
        userPreferences.setWalletAddress(jsonObject.getString(PHRASE_PARAM));
      }
      if (jsonObject.has(PRINCIPAL_ACCOUNT_PARAM)) {
        userPreferences.setPrincipalAccount(jsonObject.getString(PRINCIPAL_ACCOUNT_PARAM));
      }
      if (jsonObject.has(ENABLE_DELEGATION_PARAM)) {
        userPreferences.setEnableDelegation(jsonObject.getBoolean(ENABLE_DELEGATION_PARAM));
      }
      if (jsonObject.has(DATA_VERSION_PARAM)) {
        userPreferences.setDataVersion(jsonObject.getInt(DATA_VERSION_PARAM));
      }
      userPreferences.setOverviewAccounts(jsonArrayToList(jsonObject, OVERVIEW_ACCOUNTS_PARAM));
      return userPreferences;
    } catch (JSONException e) {
      throw new IllegalStateException("Error while converting JSON String to Object", e);
    }
  }

  @Override
  public String toString() {
    return toJSONString();
  }
}
