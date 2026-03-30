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
    String sql = "SELECT device_type, countMerge(clicks) AS total_clicks "
        + "FROM minute_analytics "
        + "WHERE url_id = ? AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY device_type ORDER BY total_clicks DESC";
    return clickHouseJdbcTemplate.query(sql, this::mapBreakdownItem, urlId);
  }

  public List<BreakdownItem> queryDeviceBreakdownByUrlIds(List<Long> urlIds, Period period) {
    String placeholders = String.join(",", urlIds.stream().map(id -> "?").toList());
    String sql = "SELECT device_type, countMerge(clicks) AS total_clicks "
        + "FROM minute_analytics "
        + "WHERE url_id IN (" + placeholders + ") AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY device_type ORDER BY total_clicks DESC";
    return clickHouseJdbcTemplate.query(sql, this::mapBreakdownItem, urlIds.toArray());
  }

  public List<BreakdownItem> queryCountryBreakdownByUrlId(Long urlId, Period period) {
    String sql = "SELECT country_code, countMerge(clicks) AS total_clicks "
        + "FROM minute_analytics "
        + "WHERE url_id = ? AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY country_code ORDER BY total_clicks DESC";
    return clickHouseJdbcTemplate.query(sql, this::mapCountryBreakdownItem, urlId);
  }

  public List<BreakdownItem> queryCountryBreakdownByUrlIds(List<Long> urlIds, Period period) {
    String placeholders = String.join(",", urlIds.stream().map(id -> "?").toList());
    String sql = "SELECT country_code, countMerge(clicks) AS total_clicks "
        + "FROM minute_analytics "
        + "WHERE url_id IN (" + placeholders + ") AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY country_code ORDER BY total_clicks DESC";
    return clickHouseJdbcTemplate.query(sql, this::mapCountryBreakdownItem, urlIds.toArray());
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

  private BreakdownItem mapBreakdownItem(ResultSet rs, int rowNum) throws SQLException {
    return BreakdownItem.builder()
        .label(rs.getString("device_type"))
        .clicks(rs.getLong("total_clicks"))
        .build();
  }

  private BreakdownItem mapCountryBreakdownItem(ResultSet rs, int rowNum) throws SQLException {
    return BreakdownItem.builder()
        .label(rs.getString("country_code"))
        .clicks(rs.getLong("total_clicks"))
        .build();
  }

  public List<BreakdownItem> queryBrowserBreakdownByUrlId(Long urlId, Period period) {
    String sql = "SELECT browser, countMerge(clicks) AS total_clicks "
        + "FROM minute_analytics "
        + "WHERE url_id = ? AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY browser ORDER BY total_clicks DESC";
    return clickHouseJdbcTemplate.query(sql, this::mapBrowserBreakdownItem, urlId);
  }

  public List<BreakdownItem> queryBrowserBreakdownByUrlIds(List<Long> urlIds, Period period) {
    String placeholders = String.join(",", urlIds.stream().map(id -> "?").toList());
    String sql = "SELECT browser, countMerge(clicks) AS total_clicks "
        + "FROM minute_analytics "
        + "WHERE url_id IN (" + placeholders + ") AND minute >= now() - INTERVAL " + period.toClickHouseInterval() + " "
        + "GROUP BY browser ORDER BY total_clicks DESC";
    return clickHouseJdbcTemplate.query(sql, this::mapBrowserBreakdownItem, urlIds.toArray());
  }

  private BreakdownItem mapBrowserBreakdownItem(ResultSet rs, int rowNum) throws SQLException {
    return BreakdownItem.builder()
        .label(rs.getString("browser"))
        .clicks(rs.getLong("total_clicks"))
        .build();
  }
}
