package com.warp.warp_backend.service;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.enums.Period;
import com.warp.warp_backend.model.exception.BaseException;
import com.warp.warp_backend.model.exception.NotFoundException;
import com.warp.warp_backend.model.response.BreakdownItem;
import com.warp.warp_backend.model.response.BreakdownResponse;
import com.warp.warp_backend.model.response.TimeSeriesDataPoint;
import com.warp.warp_backend.model.response.TimeSeriesResponse;
import com.warp.warp_backend.model.response.TopUrlTimeSeriesEntry;
import com.warp.warp_backend.model.response.TopUrlsTimeSeriesResponse;
import com.warp.warp_backend.model.response.UrlClicksAggregate;
import com.warp.warp_backend.properties.ApplicationProperties;
import com.warp.warp_backend.repository.ClickHouseAnalyticsRepository;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.service.util.UrlServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

  @Autowired
  private CurrentUserService currentUserService;

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private ClickHouseAnalyticsRepository clickHouseAnalyticsRepository;

  @Autowired
  private UrlServiceUtil urlServiceUtil;

  @Autowired
  private ApplicationProperties applicationProperties;

  public TimeSeriesResponse getClicksTimeSeries(String shortUrl, String periodValue) {
    Period period = Period.fromValue(periodValue);
    if (Objects.isNull(period)) {
      throw new BaseException(ErrorCode.INVALID_PERIOD);
    }

    Long currentUserId = currentUserService.getCurrentUserId();

    List<TimeSeriesDataPoint> rawData;
    if (Objects.nonNull(shortUrl)) {
      Url url = urlRepository.findByShortUrlAndDeletedDateIsNull(shortUrl)
          .orElseThrow(() -> new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND));
      if (!url.getUserId().equals(currentUserId)) {
        throw new BaseException(ErrorCode.URL_ACCESS_FORBIDDEN);
      }
      rawData = clickHouseAnalyticsRepository.queryTimeSeriesByUrlId(url.getId(), period);
    } else {
      List<Long> urlIds = urlRepository.findAllIdsByUserId(currentUserId);
      rawData = clickHouseAnalyticsRepository.queryTimeSeriesByUrlIds(urlIds, period);
    }

    List<TimeSeriesDataPoint> filled = zeroFill(rawData, period);
    return TimeSeriesResponse.builder()
        .period(period.getValue())
        .bucket(period.getBucket())
        .data(filled)
        .build();
  }

  public TopUrlsTimeSeriesResponse getTopUrlsTimeSeries(String periodValue) {
    Period period = Period.fromValue(periodValue);
    if (Objects.isNull(period)) {
      throw new BaseException(ErrorCode.INVALID_PERIOD);
    }

    Long currentUserId = currentUserService.getCurrentUserId();
    List<Long> userUrlIds = urlRepository.findAllIdsByUserId(currentUserId);

    if (userUrlIds.isEmpty()) {
      return TopUrlsTimeSeriesResponse.builder()
          .period(period.getValue())
          .bucket(period.getBucket())
          .urls(Collections.emptyList())
          .build();
    }

    List<UrlClicksAggregate> topUrls = clickHouseAnalyticsRepository.queryTopUrlsByClicks(
        userUrlIds, period, applicationProperties.getTopUrlsMaxLimit()).stream()
        .filter(u -> u.getTotalClicks() > 0)
        .toList();

    if (topUrls.isEmpty()) {
      return TopUrlsTimeSeriesResponse.builder()
          .period(period.getValue())
          .bucket(period.getBucket())
          .urls(Collections.emptyList())
          .build();
    }

    List<Long> topUrlIds = topUrls.stream().map(UrlClicksAggregate::getUrlId).toList();

    Map<Long, String> shortUrlById = urlRepository.findAllById(topUrlIds).stream()
        .collect(Collectors.toMap(Url::getId, url -> urlServiceUtil.formatUrl(url.getShortUrl())));

    List<TopUrlTimeSeriesEntry> entries = topUrls.stream()
        .map(urlTotal -> {
          List<TimeSeriesDataPoint> raw = clickHouseAnalyticsRepository.queryTimeSeriesByUrlId(urlTotal.getUrlId(), period);
          List<TimeSeriesDataPoint> filled = zeroFill(raw, period);
          return TopUrlTimeSeriesEntry.builder()
              .shortUrl(shortUrlById.get(urlTotal.getUrlId()))
              .totalClicks(urlTotal.getTotalClicks())
              .timeseries(filled)
              .build();
        })
        .toList();

    return TopUrlsTimeSeriesResponse.builder()
        .period(period.getValue())
        .bucket(period.getBucket())
        .urls(entries)
        .build();
  }

  public BreakdownResponse getDeviceBreakdown(String shortUrl, String periodValue) {
    Period period = parsePeriod(periodValue);
    Long currentUserId = currentUserService.getCurrentUserId();
    return getBreakdown(shortUrl, period, currentUserId,
        id -> clickHouseAnalyticsRepository.queryDeviceBreakdownByUrlId(id, period),
        ids -> clickHouseAnalyticsRepository.queryDeviceBreakdownByUrlIds(ids, period));
  }

  public BreakdownResponse getCountryBreakdown(String shortUrl, String periodValue) {
    Period period = parsePeriod(periodValue);
    Long currentUserId = currentUserService.getCurrentUserId();
    return getBreakdown(shortUrl, period, currentUserId,
        id -> clickHouseAnalyticsRepository.queryCountryBreakdownByUrlId(id, period),
        ids -> clickHouseAnalyticsRepository.queryCountryBreakdownByUrlIds(ids, period));
  }

  public BreakdownResponse getBrowserBreakdown(String shortUrl, String periodValue) {
    Period period = parsePeriod(periodValue);
    Long currentUserId = currentUserService.getCurrentUserId();
    return getBreakdown(shortUrl, period, currentUserId,
        id -> clickHouseAnalyticsRepository.queryBrowserBreakdownByUrlId(id, period),
        ids -> clickHouseAnalyticsRepository.queryBrowserBreakdownByUrlIds(ids, period));
  }

  public BreakdownResponse getSourceBreakdown(String shortUrl, String periodValue) {
    Period period = parsePeriod(periodValue);
    Long currentUserId = currentUserService.getCurrentUserId();
    return getBreakdown(shortUrl, period, currentUserId,
        id -> clickHouseAnalyticsRepository.querySourceBreakdownByUrlId(id, period),
        ids -> clickHouseAnalyticsRepository.querySourceBreakdownByUrlIds(ids, period));
  }

  private Period parsePeriod(String periodValue) {
    Period period = Period.fromValue(periodValue);
    if (Objects.isNull(period)) {
      throw new BaseException(ErrorCode.INVALID_PERIOD);
    }
    return period;
  }

  private BreakdownResponse getBreakdown(String shortUrl, Period period, Long currentUserId,
      Function<Long, List<BreakdownItem>> queryByUrlId, Function<List<Long>, List<BreakdownItem>> queryByUrlIds) {

    List<BreakdownItem> items;
    if (Objects.nonNull(shortUrl)) {
      Url url = urlRepository.findByShortUrlAndDeletedDateIsNull(shortUrl)
          .orElseThrow(() -> new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND));
      if (!url.getUserId().equals(currentUserId)) {
        throw new BaseException(ErrorCode.URL_ACCESS_FORBIDDEN);
      }
      items = queryByUrlId.apply(url.getId());
    } else {
      List<Long> userUrlIds = urlRepository.findAllIdsByUserId(currentUserId);
      if (userUrlIds.isEmpty()) {
        return BreakdownResponse.builder()
            .period(period.getValue())
            .totalClicks(0)
            .items(Collections.emptyList())
            .build();
      }
      items = queryByUrlIds.apply(userUrlIds);
    }

    long totalClicks = items.stream().mapToLong(BreakdownItem::getClicks).sum();
    if (totalClicks == 0) {
      return BreakdownResponse.builder()
          .period(period.getValue())
          .totalClicks(0)
          .items(Collections.emptyList())
          .build();
    }

    List<BreakdownItem> enriched = items.stream()
        .map(item -> BreakdownItem.builder()
            .label(item.getLabel())
            .clicks(item.getClicks())
            .percentage(Math.round(item.getClicks() * 1000.0 / totalClicks) / 10.0)
            .build())
        .toList();

    return BreakdownResponse.builder()
        .period(period.getValue())
        .totalClicks(totalClicks)
        .items(enriched)
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
    Instant end = truncateToBucket(now, period);
    Duration step = period.stepDuration();

    List<TimeSeriesDataPoint> result = new ArrayList<>();
    for (Instant cursor = start; !cursor.isAfter(end); cursor = cursor.plus(step)) {
      result.add(TimeSeriesDataPoint.builder()
          .timestamp(cursor)
          .clicks(clicksByTimestamp.getOrDefault(cursor, 0L))
          .build());
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
