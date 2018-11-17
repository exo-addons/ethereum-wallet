package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TransactionDetail implements Serializable {

  private static final long serialVersionUID = 658273092293607458L;

  private Long              networkId;

  private String            hash;

  private String            contractAddress;

  private String            contractMethodName;

  private boolean           pending;

  private boolean           isAdminOperation;

  private String            from;

  private String            to;

  private String            label;

  private String            message;

  private double            value;

  private double            contractAmount;

  private long              timestamp;

  public static TransactionDetail fromStoredValue(String storedTransactionDetails) {
    TransactionDetail transactionMessage = new TransactionDetail();
    if (StringUtils.isNotBlank(storedTransactionDetails)) {
      String[] transactionDetailsArray = storedTransactionDetails.split(";");
      transactionMessage.setHash(transactionDetailsArray[0]);
      transactionMessage.setLabel(transactionDetailsArray.length > 1 ? decode(transactionDetailsArray[1]) : null);
      transactionMessage.setMessage(transactionDetailsArray.length > 2 ? decode(transactionDetailsArray[2]) : null);
    }
    return transactionMessage;
  }

  /**
   * Determine the value to store on address transactions list if sender,
   * include label, else omit it.
   * 
   * @param sender
   * @return
   */
  public String getToStoreValue(boolean sender) {
    return hash + ";" + (sender ? encode(label) : "") + ";" + encode(message);
  }

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      if (networkId != null && networkId > 0) {
        jsonObject.put("networkId", networkId);
      }
      jsonObject.put("hash", hash);
      if (StringUtils.isNotBlank(contractAddress)) {
        jsonObject.put("contractAddress", contractAddress);
      }
      if (StringUtils.isNotBlank(contractMethodName)) {
        jsonObject.put("contractMethodName", contractMethodName);
      }
      if (StringUtils.isNotBlank(from)) {
        jsonObject.put("from", from);
      }
      if (StringUtils.isNotBlank(to)) {
        jsonObject.put("to", to);
      }
      jsonObject.put("label", label);
      jsonObject.put("message", message);
      if (value > 0) {
        jsonObject.put("value", value);
      }
      if (contractAmount > 0) {
        jsonObject.put("contractAmount", contractAmount);
      }
      if (timestamp > 0) {
        jsonObject.put("timestamp", timestamp);
      }
      if (pending) {
        jsonObject.put("pending", pending);
      }
      if (isAdminOperation) {
        jsonObject.put("isAdminOperation", isAdminOperation);
      }
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  private static String encode(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLEncoder.encode(content.trim(), "UTF-8");
    } catch (Exception e) {
      return content;
    }
  }

  private static String decode(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLDecoder.decode(content.trim(), "UTF-8");
    } catch (Exception e) {
      return content;
    }
  }
}
