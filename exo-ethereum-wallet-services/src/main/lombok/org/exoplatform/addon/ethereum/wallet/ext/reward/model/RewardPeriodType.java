package org.exoplatform.addon.ethereum.wallet.ext.reward.model;

import static org.exoplatform.addon.ethereum.wallet.service.utils.GamificationUtils.timeToSeconds;

import java.time.*;

public enum RewardPeriodType {
  WEEK, MONTH, QUARTER, SEMESTER, YEAR;

  public static final RewardPeriodType DEFAULT = MONTH;

  public RewardPeriod getPeriodOfTime(LocalDateTime localDateTime) {
    RewardPeriod gamificationPeriod = new RewardPeriod();
    YearMonth yearMonth = YearMonth.from(localDateTime);
    switch (this) {
    case WEEK:
      LocalDateTime firstDayOfThisWeek = localDateTime.with(DayOfWeek.MONDAY);
      LocalDateTime firstDayOfNextWeek = firstDayOfThisWeek.plusWeeks(1);
      gamificationPeriod.setStartDateInSeconds(timeToSeconds(firstDayOfThisWeek));
      gamificationPeriod.setEndDateInSeconds(timeToSeconds(firstDayOfNextWeek));
      break;
    case MONTH:
      YearMonth currentMonth = yearMonth;
      YearMonth nextMonth = currentMonth.plusMonths(1);
      gamificationPeriod.setStartDateInSeconds(timeToSeconds(currentMonth.atDay(1).atStartOfDay()));
      gamificationPeriod.setEndDateInSeconds(timeToSeconds(nextMonth.atDay(1).atStartOfDay()));
      break;
    case QUARTER:
      int monthQuarterIndex = (((int) yearMonth.getMonthValue() - 1) / 3) * 3 + 1;

      YearMonth startQuarterMonth = YearMonth.of(yearMonth.getYear(), monthQuarterIndex);
      YearMonth endQuarterMonth = startQuarterMonth.plusMonths(3);
      gamificationPeriod.setStartDateInSeconds(timeToSeconds(startQuarterMonth.atDay(1).atStartOfDay()));
      gamificationPeriod.setEndDateInSeconds(timeToSeconds(endQuarterMonth.atDay(1).atStartOfDay()));
      break;
    case SEMESTER:
      int monthSemesterIndex = (((int) yearMonth.getMonthValue() - 1) / 6) * 6 + 1;

      YearMonth startSemesterMonth = YearMonth.of(yearMonth.getYear(), monthSemesterIndex);
      YearMonth endSemesterMonth = startSemesterMonth.plusMonths(6);
      gamificationPeriod.setStartDateInSeconds(timeToSeconds(startSemesterMonth.atDay(1).atStartOfDay()));
      gamificationPeriod.setEndDateInSeconds(timeToSeconds(endSemesterMonth.atDay(1).atStartOfDay()));
      break;
    case YEAR:
      gamificationPeriod.setStartDateInSeconds(timeToSeconds(Year.from(localDateTime).atDay(1).atStartOfDay()));
      gamificationPeriod.setEndDateInSeconds(timeToSeconds(Year.from(localDateTime).plusYears(1).atDay(1).atStartOfDay()));
      break;
    }
    return gamificationPeriod;
  }
}
