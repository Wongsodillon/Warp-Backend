package com.warp.warp_backend.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.Properties;

@Configuration
public class ClickHouseConfig {

  @Value("${clickhouse.jdbc.url}")
  private String jdbcUrl;

  @Value("${clickhouse.jdbc.username}")
  private String username;

  @Value("${clickhouse.jdbc.password}")
  private String password;

  @Bean("clickHouseJdbcTemplate")
  public JdbcTemplate clickHouseJdbcTemplate() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("user", username);
    properties.setProperty("password", password);
    return new JdbcTemplate(new ClickHouseDataSource(jdbcUrl, properties));
  }
}