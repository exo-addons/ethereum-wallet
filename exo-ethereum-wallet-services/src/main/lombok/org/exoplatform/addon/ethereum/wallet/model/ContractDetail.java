package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Data;

@Data
public class ContractDetail implements Serializable {

  private static final long serialVersionUID = 1459881604949041768L;

  private String            address;

  private String            name;

  private String            symbol;

  private Long              networkId;

  public ContractDetail() {
  }

  public ContractDetail(Long networkId, String address, String name, String symbol) {
    this.networkId = networkId;
    this.address = address;
    this.name = name;
    this.symbol = symbol;
  }

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("address", address);
      jsonObject.put("name", name);
      jsonObject.put("symbol", symbol);
      jsonObject.put("networkId", networkId);
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
