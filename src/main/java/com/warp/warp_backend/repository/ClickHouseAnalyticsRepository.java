package com.warp.warp_backend.repository;

import com.warp.warp_backend.model.enums.Period;
import com.warp.warp_backend.model.response.BreakdownItem;
import com.warp.warp_backend.model.response.TimeSeriesDataPoint;
import com.warp.warp_backend.model.response.UrlClicksAggregate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Repository
public class ClickHouseAnalyticsRepository {

  private final JdbcTemplate clickHouseJdbcTemplate;

  public ClickHouseAnalyticsRepository(
      @Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouseJdbcTemplate) {
    this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
  }

  public List<TimeSeriesDataPoint> queryTimeSeriesByUrlId(Long urlId, Period period) {
    String sql = "SELECT toUnixTimestamp(" + period.getBucketExpr() + ") AS ts, countMerge(clicks) AS clicks "
        + "FROM minute_analytics "
        + "WHERE url_id = ? AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY ts ORDER BY ts";
    return clickHouseJdbcTemplate.query(sql, this::mapRow, urlId);
  }

  public List<TimeSeriesDataPoint> queryTimeSeriesByUrlIds(List<Long> urlIds, Period period) {
    if (urlIds.isEmpty()) {
      return Collections.emptyList();
    }
    String placeholders = String.join(",", urlIds.stream().map(id -> "?").toList());
    String sql = "SELECT toUnixTimestamp(" + period.getBucketExpr() + ") AS ts, countMerge(clicks) AS clicks "
        + "FROM minute_analytics "
        + "WHERE url_id IN (" + placeholders + ") AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY ts ORDER BY ts";
    return clickHouseJdbcTemplate.query(sql, this::mapRow, urlIds.toArray());
  }

  public List<UrlClicksAggregate> queryTopUrlsByClicks(List<Long> urlIds, Period period, int limit) {
    String placeholders = String.join(",", urlIds.stream().map(id -> "?").toList());
    String sql = "SELECT url_id, countMerge(clicks) AS total_clicks "
        + "FROM minute_analytics "
        + "WHERE url_id IN (" + placeholders + ") AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY url_id "
        + "ORDER BY total_clicks DESC "
        + "LIMIT " + limit;
    return clickHouseJdbcTemplate.query(sql, this::mapUrlClicksAggregate, urlIds.toArray());
  }

  public List<BreakdownItem> queryDeviceBreakdownByUrlId(Long urlId, Period period) {
    return queryBreakdownByUrlId(urlId, period, "device_type");
  }

  public List<BreakdownItem> queryDeviceBreakdownByUrlIds(List<Long> urlIds, Period period) {
    return queryBreakdownByUrlIds(urlIds, period, "device_type");
  }

  public List<BreakdownItem> queryCountryBreakdownByUrlId(Long urlId, Period period) {
    return queryBreakdownByUrlId(urlId, period, "country_code");
  }

  public List<BreakdownItem> queryCountryBreakdownByUrlIds(List<Long> urlIds, Period period) {
    return queryBreakdownByUrlIds(urlIds, period, "country_code");
  }

  public List<BreakdownItem> queryBrowserBreakdownByUrlId(Long urlId, Period period) {
    return queryBreakdownByUrlId(urlId, period, "browser");
  }

  public List<BreakdownItem> queryBrowserBreakdownByUrlIds(List<Long> urlIds, Period period) {
    return queryBreakdownByUrlIds(urlIds, period, "browser");
  }

  public List<BreakdownItem> querySourceBreakdownByUrlId(Long urlId, Period period) {
    return queryBreakdownByUrlId(urlId, period, "referrer");
  }

  public List<BreakdownItem> querySourceBreakdownByUrlIds(List<Long> urlIds, Period period) {
    return queryBreakdownByUrlIds(urlIds, period, "referrer");
  }

  private List<BreakdownItem> queryBreakdownByUrlId(Long urlId, Period period, String column) {
    String sql = "SELECT " + column + ", countMerge(clicks) AS total_clicks "
        + "FROM minute_analytics "
        + "WHERE url_id = ? AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY " + column + " ORDER BY total_clicks DESC";
    return clickHouseJdbcTemplate.query(sql, (rs, rowNum) -> mapBreakdownItem(rs, column), urlId);
  }

  private List<BreakdownItem> queryBreakdownByUrlIds(List<Long> urlIds, Period period, String column) {
    String placeholders = String.join(",", urlIds.stream().map(id -> "?").toList());
    String sql = "SELECT " + column + ", countMerge(clicks) AS total_clicks "
        + "FROM minute_analytics "
        + "WHERE url_id IN (" + placeholders + ") AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY " + column + " ORDER BY total_clicks DESC";
    return clickHouseJdbcTemplate.query(sql, (rs, rowNum) -> mapBreakdownItem(rs, column), urlIds.toArray());
  }

  private BreakdownItem mapBreakdownItem(ResultSet rs, String column) throws SQLException {
    return BreakdownItem.builder()
        .label(rs.getString(column))
        .clicks(rs.getLong("total_clicks"))
        .build();
  }

  private TimeSeriesDataPoint mapRow(ResultSet rs, int rowNum) throws SQLException {
    return TimeSeriesDataPoint.builder()
        .timestamp(Instant.ofEpochSecond(rs.getLong("ts")))
        .clicks(rs.getLong("clicks"))
        .build();
  }

  private UrlClicksAggregate mapUrlClicksAggregate(ResultSet rs, int rowNum) throws SQLException {
    return UrlClicksAggregate.builder()
        .urlId(rs.getLong("url_id"))
        .totalClicks(rs.getLong("total_clicks"))
        .build();
  }
}
