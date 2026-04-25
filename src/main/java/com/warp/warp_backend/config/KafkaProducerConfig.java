package com.warp.warp_backend.config;

import com.warp.warp_backend.model.event.UrlClickEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {

  private final KafkaProperties kafkaProperties;

  public KafkaProducerConfig(KafkaProperties kafkaProperties) {
    this.kafkaProperties = kafkaProperties;
  }

  @Bean
  public ProducerFactory<String, UrlClickEvent> urlClickEventProducerFactory() {
    Map<String, Object> props = kafkaProperties.buildProducerProperties(null);
    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "5000");
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, UrlClickEvent> urlClickEventKafkaTemplate() {
    return new KafkaTemplate<>(urlClickEventProducerFactory());
  }
}
