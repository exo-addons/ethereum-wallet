package org.exoplatform.addon.ethereum.wallet.ext.gamification.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.GamificationUtils.*;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.ObjectBuilder;

import lombok.Data;

@Data
public class GamificationSettings implements Serializable {

  public static final long       serialVersionUID = 2416168589742260549L;

  private long                   threshold;

  private String                 contractAddress;

  private GamificationPeriodType periodType       = GamificationPeriodType.DEFAULT;

  private GamificationRewardType rewardType       = GamificationRewardType.FIXED;

  private double                 totalBudget;

  private double                 budgetPerMember;

  public static GamificationSettings fromString(String value) throws Exception {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    JsonDefaultHandler jsonDefaultHandler = new JsonDefaultHandler();
    JSON_PARSER.parse(new ByteArrayInputStream(value.getBytes()), jsonDefaultHandler);
    return ObjectBuilder.createObject(GamificationSettings.class, jsonDefaultHandler.getJsonObject());
  }

  public String toStringToStore() {
    try {
      return JSON_GENERATOR.createJsonObject(this).toString();
    } catch (Exception e) {
      throw new RuntimeException("Can't transform current GamificationSettings to string", e);
    }
  }
}
