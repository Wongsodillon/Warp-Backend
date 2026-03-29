package com.warp.warp_backend.service;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.enums.Period;
import com.warp.warp_backend.model.exception.BaseException;
import com.warp.warp_backend.model.exception.NotFoundException;
import com.warp.warp_backend.model.response.TimeSeriesDataPoint;
import com.warp.warp_backend.model.response.TimeSeriesResponse;
import com.warp.warp_backend.repository.ClickHouseAnalyticsRepository;
import com.warp.warp_backend.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

  @Autowired
  private CurrentUserService currentUserService;

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private ClickHouseAnalyticsRepository clickHouseAnalyticsRepository;

  public TimeSeriesResponse getClicksTimeSeries(Long urlId, String periodValue) {
    Period period = Period.fromValue(periodValue);
    if (Objects.isNull(period)) {
      throw new BaseException(ErrorCode.INVALID_PERIOD);
    }

    Long currentUserId = currentUserService.getCurrentUserId();

    List<TimeSeriesDataPoint> rawData;
    if (Objects.nonNull(urlId)) {
      Url url = urlRepository.findById(urlId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND));
      if (!url.getUserId().equals(currentUserId)) {
        throw new BaseException(ErrorCode.URL_ACCESS_FORBIDDEN);
      }
      rawData = clickHouseAnalyticsRepository.queryTimeSeriesByUrlId(urlId, period);
      System.out.println("Raw Data: " + rawData);
    } else {
      List<Long> urlIds = urlRepository.findAllIdsByUserId(currentUserId);
      rawData = clickHouseAnalyticsRepository.queryTimeSeriesByUrlIds(urlIds, period);
    }

    List<TimeSeriesDataPoint> filled = zeroFill(rawData, period);
    System.out.println("Filled Data: " + filled);
    return TimeSeriesResponse.builder()
        .period(period.getValue())
        .bucket(period.getBucket())
        .data(filled)
        .build();
  }

  private List<TimeSeriesDataPoint> zeroFill(List<TimeSeriesDataPoint> rawData, Period period) {
    Map<Instant, Long> clicksByTimestamp = rawData.stream()
        .collect(Collectors.toMap(
            TimeSeriesDataPoint::getTimestamp,
            TimeSeriesDataPoint::getClicks,
            Long::sum
        ));

    Instant now = Instant.now();
    Instant start = truncateToBucket(now.minus(period.getDuration()), period);
    Duration step = period.stepDuration();
    int points = period.getExpectedPoints();

    List<TimeSeriesDataPoint> result = new ArrayList<>(points);
    Instant cursor = start;
    for (int i = 0; i < points; i++) {
      long clicks = clicksByTimestamp.getOrDefault(cursor, 0L);
      result.add(TimeSeriesDataPoint.builder()
          .timestamp(cursor)
          .clicks(clicks)
          .build());
      cursor = cursor.plus(step);
    }

    return result;
  }

  private Instant truncateToBucket(Instant instant, Period period) {
    return switch (period) {
      case ONE_HOUR -> instant.truncatedTo(ChronoUnit.MINUTES);
      case SIX_HOURS -> {
        Instant truncatedToMinute = instant.truncatedTo(ChronoUnit.MINUTES);
        long minuteOfHour = truncatedToMinute.atZone(ZoneOffset.UTC).getMinute();
        long excess = minuteOfHour % 15;
        yield truncatedToMinute.minus(excess, ChronoUnit.MINUTES);
      }
      case ONE_DAY, SEVEN_DAYS -> instant.truncatedTo(ChronoUnit.HOURS);
      case THIRTY_DAYS -> instant.truncatedTo(ChronoUnit.DAYS);
    };
  }
}
