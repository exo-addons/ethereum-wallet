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
public class TransactionMessage implements Serializable {

  private static final long serialVersionUID = 658273092293607458L;

  private String            hash;

  private String            label;

  private String            message;

  private String            sender;

  public TransactionMessage(String transactionDetails) {
    if (StringUtils.isNotBlank(transactionDetails)) {
      String[] transactionDetailsArray = transactionDetails.split(";");
      this.hash = transactionDetailsArray[0];
      this.label = transactionDetailsArray.length > 1 ? decode(transactionDetailsArray[1]) : null;
      this.message = transactionDetailsArray.length > 2 ? decode(transactionDetailsArray[2]) : null;
    }
  }

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("hash", hash);
      jsonObject.put("label", label);
      jsonObject.put("message", message);
      jsonObject.put("sender", sender);
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    return hash + ";" + encode(label) + ";" + encode(message);
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
