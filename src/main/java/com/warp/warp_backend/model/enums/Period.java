package com.warp.warp_backend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

@Getter
@AllArgsConstructor
public enum Period {

  ONE_HOUR("1h", "minute", "minute", Duration.ofHours(1), 60),
  SIX_HOURS("6h", "fifteen_minutes", "toStartOfInterval(minute, INTERVAL 15 MINUTE)", Duration.ofHours(6), 24),
  ONE_DAY("1d", "hour", "toStartOfHour(minute)", Duration.ofDays(1), 24),
  SEVEN_DAYS("7d", "hour", "toStartOfHour(minute)", Duration.ofDays(7), 168),
  THIRTY_DAYS("30d", "day", "toStartOfDay(minute, 'UTC')", Duration.ofDays(30), 30);

  private final String value;
  private final String bucket;
  private final String bucketExpr;
  private final Duration duration;
  private final int expectedPoints;

  public static Period fromValue(String value) {
    for (Period p : values()) {
      if (p.value.equals(value)) {
        return p;
      }
    }
    return null;
  }

  public String toClickHouseInterval() {
    if (duration.toDays() > 0) {
      return duration.toDays() + " DAY";
    }
    return duration.toHours() + " HOUR";
  }

  public Duration stepDuration() {
    return switch (this) {
      case ONE_HOUR -> Duration.ofMinutes(1);
      case SIX_HOURS -> Duration.ofMinutes(15);
      case ONE_DAY, SEVEN_DAYS -> Duration.ofHours(1);
      case THIRTY_DAYS -> Duration.ofDays(1);
    };
  }
}
