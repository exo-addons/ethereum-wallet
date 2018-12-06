package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractDetail implements Serializable {

  private static final long serialVersionUID = 1459881604949041768L;

  private String            address;

  private String            name;

  private String            symbol;

  private Integer           decimals;

  private Long              networkId;

  private String            owner;

  private String            sellPrice;

  private String            contractType;

  private boolean           defaultContract;

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      if (StringUtils.isNotBlank(name)) {
        jsonObject.put("address", address);
      }
      if (StringUtils.isNotBlank(name)) {
        jsonObject.put("name", name);
      }
      if (StringUtils.isNotBlank(symbol)) {
        jsonObject.put("symbol", symbol);
      }
      if (decimals != null) {
        jsonObject.put("decimals", decimals);
      }
      if (networkId != null) {
        jsonObject.put("networkId", networkId);
      }
      if (StringUtils.isNotBlank(owner)) {
        jsonObject.put("owner", owner);
      }
      if (StringUtils.isNotBlank(sellPrice)) {
        jsonObject.put("sellPrice", sellPrice);
      }
      if (StringUtils.isNotBlank(contractType)) {
        jsonObject.put("contractType", contractType);
      }
      jsonObject.put("defaultContract", defaultContract);
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  public static final ContractDetail parseStringToObject(String jsonString) {
    if (StringUtils.isBlank(jsonString)) {
      return null;
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      ContractDetail contractDetail = new ContractDetail();
      contractDetail.setNetworkId(jsonObject.has("networkId") ? jsonObject.getLong("networkId") : 0);
      contractDetail.setAddress(jsonObject.has("address") ? jsonObject.getString("address") : null);
      contractDetail.setName(jsonObject.has("name") ? jsonObject.getString("name") : null);
      contractDetail.setSymbol(jsonObject.has("symbol") ? jsonObject.getString("symbol") : null);
      contractDetail.setDecimals(jsonObject.has("decimals") ? jsonObject.getInt("decimals") : 0);
      contractDetail.setOwner(jsonObject.has("owner") ? jsonObject.getString("owner") : null);
      contractDetail.setSellPrice(jsonObject.has("sellPrice") ? jsonObject.getString("sellPrice") : null);
      contractDetail.setContractType(jsonObject.has("contractType") ? jsonObject.getString("contractType") : null);
      contractDetail.setDefaultContract(jsonObject.has("defaultContract") ? jsonObject.getBoolean("defaultContract") : true);
      return contractDetail;
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting JSON String to Object", e);
    }
  }

  @Override
  public String toString() {
    return toJSONString();
  }
}
