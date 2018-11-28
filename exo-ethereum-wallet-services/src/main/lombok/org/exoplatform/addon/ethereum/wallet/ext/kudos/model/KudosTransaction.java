package org.exoplatform.addon.ethereum.wallet.ext.kudos.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.decodeString;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.encodeString;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class KudosTransaction implements Serializable {

  private static final long serialVersionUID = 658273092293607458L;

  private Long              networkId;

  private String            periodType;

  private long              startDateInSeconds;

  private String            hash;

  private String            receiverType;

  private String            receiverId;

  private String            tokensAmountSent;

  public static KudosTransaction fromStoredValue(String storedTransactionDetails) {
    KudosTransaction transactionMessage = new KudosTransaction();
    if (StringUtils.isNotBlank(storedTransactionDetails)) {
      String[] transactionDetailsArray = storedTransactionDetails.split(";");
      transactionMessage.setHash(transactionDetailsArray[0]);
      transactionMessage.setReceiverType(transactionDetailsArray.length > 1 ? decodeString(transactionDetailsArray[1]) : null);
      transactionMessage.setReceiverId(transactionDetailsArray.length > 2 ? decodeString(transactionDetailsArray[2]) : null);
      transactionMessage.setTokensAmountSent(transactionDetailsArray.length > 3 ? decodeString(transactionDetailsArray[3])
                                                                                : null);
    }
    return transactionMessage;
  }

  /**
   * Determine the value to store on transactions list.
   * 
   * @return
   */
  public String getToStoreValue() {
    if (StringUtils.isBlank(receiverType)) {
      throw new IllegalStateException("receiverType is mandatory");
    }
    if (StringUtils.isBlank(receiverId)) {
      throw new IllegalStateException("receiverId is mandatory");
    }
    return hash + ";" + encodeString(receiverType) + ";" + encodeString(receiverId) + ";" + encodeString(tokensAmountSent);
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
      if (StringUtils.isNotBlank(hash)) {
        jsonObject.put("hash", hash);
      }
      if (StringUtils.isNotBlank(receiverType)) {
        jsonObject.put("receiverType", receiverType);
      }
      if (StringUtils.isNotBlank(receiverId)) {
        jsonObject.put("receiverId", receiverId);
      }
      if (StringUtils.isNotBlank(tokensAmountSent)) {
        jsonObject.put("tokensAmountSent", tokensAmountSent);
      }
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof KudosTransaction)) {
      return false;
    }
    return StringUtils.equalsIgnoreCase(hash, ((KudosTransaction) obj).getHash());
  }
}
