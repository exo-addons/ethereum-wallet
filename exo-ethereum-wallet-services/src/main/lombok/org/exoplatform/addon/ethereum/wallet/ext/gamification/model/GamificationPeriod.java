package org.exoplatform.addon.ethereum.wallet.ext.gamification.model;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamificationPeriod {

  private long startDateInSeconds;

  private long endDateInSeconds;

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("startDateInSeconds", startDateInSeconds);
      jsonObject.put("endDateInSeconds", endDateInSeconds);
    } catch (JSONException e) {
      throw new RuntimeException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    return toJSONObject().toString();
  }

}
