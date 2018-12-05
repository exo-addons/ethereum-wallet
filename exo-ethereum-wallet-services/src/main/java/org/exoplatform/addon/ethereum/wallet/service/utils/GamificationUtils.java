package org.exoplatform.addon.ethereum.wallet.service.utils;

import java.time.*;
import java.util.TimeZone;

import org.exoplatform.addon.ethereum.wallet.ext.gamification.model.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class GamificationUtils {
  private static final Log LOG = ExoLogger.getLogger(GamificationUtils.class);

  public static LocalDateTime timeFromSeconds(long createdDate) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(createdDate), TimeZone.getDefault().toZoneId());
  }

  public static long timeToSeconds(LocalDateTime time) {
    return time.atZone(ZoneOffset.systemDefault()).toEpochSecond();
  }

  public static GamificationPeriod getCurrentPeriod(GamificationSettings gamificationSettings) {
    return getPeriodOfTime(gamificationSettings, LocalDateTime.now());
  }

  public static GamificationPeriod getPeriodOfTime(GamificationSettings gamificationSettings, LocalDateTime localDateTime) {
    GamificationPeriodType gamificationPeriodType = null;
    if (gamificationSettings == null || gamificationSettings.getPeriodType() == null) {
      LOG.warn("Provided gamificationSettings doesn't have a parametred gamification period type, using MONTH period type: "
          + gamificationSettings, new RuntimeException());
      gamificationPeriodType = GamificationPeriodType.DEFAULT;
    } else {
      gamificationPeriodType = gamificationSettings.getPeriodType();
    }
    return gamificationPeriodType.getPeriodOfTime(localDateTime);
  }

}
