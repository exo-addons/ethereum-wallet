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

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("address", address);
      jsonObject.put("name", name);
      jsonObject.put("symbol", symbol);
      jsonObject.put("decimals", decimals);
      jsonObject.put("networkId", networkId);
      jsonObject.put("owner", owner);
      jsonObject.put("sellPrice", sellPrice);
      jsonObject.put("contractType", contractType);
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
      contractDetail.setNetworkId(jsonObject.getLong("networkId"));
      contractDetail.setAddress(jsonObject.getString("address"));
      contractDetail.setName(jsonObject.getString("name"));
      contractDetail.setSymbol(jsonObject.getString("symbol"));
      contractDetail.setDecimals(jsonObject.has("decimals") ? jsonObject.getInt("decimals") : 0);
      contractDetail.setOwner(jsonObject.has("owner") ? jsonObject.getString("owner") : null);
      contractDetail.setSellPrice(jsonObject.has("sellPrice") ? jsonObject.getString("sellPrice") : null);
      contractDetail.setContractType(jsonObject.has("contractType") ? jsonObject.getString("contractType") : null);
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
