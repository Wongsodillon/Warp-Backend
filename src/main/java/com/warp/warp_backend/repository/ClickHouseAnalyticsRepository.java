package com.warp.warp_backend.repository;

import com.warp.warp_backend.model.enums.Period;
import com.warp.warp_backend.model.response.TimeSeriesDataPoint;
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

  private TimeSeriesDataPoint mapRow(ResultSet rs, int rowNum) throws SQLException {
    return TimeSeriesDataPoint.builder()
        .timestamp(Instant.ofEpochSecond(rs.getLong("ts")))
        .clicks(rs.getLong("clicks"))
        .build();
  }
}
