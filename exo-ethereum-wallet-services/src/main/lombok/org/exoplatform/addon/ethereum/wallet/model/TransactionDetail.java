package org.exoplatform.addon.ethereum.wallet.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.decodeString;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.encodeString;

import java.io.Serializable;

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
      transactionMessage.setLabel(transactionDetailsArray.length > 1 ? decodeString(transactionDetailsArray[1]) : null);
      transactionMessage.setMessage(transactionDetailsArray.length > 2 ? decodeString(transactionDetailsArray[2]) : null);
    }
    return transactionMessage;
  }

  public TransactionDetail copy() {
    return new TransactionDetail(networkId,
                                 hash,
                                 contractAddress,
                                 contractMethodName,
                                 pending,
                                 isAdminOperation,
                                 from,
                                 to,
                                 label,
                                 message,
                                 value,
                                 contractAmount,
                                 timestamp);
  }

  /**
   * Determine the value to store on address transactions list if sender,
   * include label, else omit it.
   * 
   * @param sender
   * @return
   */
  public String getToStoreValue(boolean sender) {
    return hash + ";" + (sender ? encodeString(label) : "") + ";" + encodeString(message);
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
      throw new IllegalStateException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }
}
