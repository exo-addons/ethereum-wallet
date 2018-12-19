package org.exoplatform.addon.ethereum.wallet.ext.reward.model;

import java.time.LocalDateTime;

import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.GamificationSettings;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardPeriod {

  private long startDateInSeconds;

  private long endDateInSeconds;

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("startDateInSeconds", startDateInSeconds);
      jsonObject.put("endDateInSeconds", endDateInSeconds);
    } catch (JSONException e) {
      throw new IllegalStateException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    return toJSONObject().toString();
  }

  public static RewardPeriod getCurrentPeriod(GamificationSettings gamificationSettings) {
    return getPeriodOfTime(gamificationSettings, LocalDateTime.now());
  }

  public static RewardPeriod getPeriodOfTime(GamificationSettings gamificationSettings, LocalDateTime localDateTime) {
    RewardPeriodType gamificationPeriodType = null;
    if (gamificationSettings == null || gamificationSettings.getPeriodType() == null) {
      gamificationPeriodType = RewardPeriodType.DEFAULT;
    } else {
      gamificationPeriodType = gamificationSettings.getPeriodType();
    }
    return gamificationPeriodType.getPeriodOfTime(localDateTime);
  }

}
