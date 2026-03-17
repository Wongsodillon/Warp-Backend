package com.warp.warp_backend.service;

import com.warp.warp_backend.model.constant.KafkaTopic;
import com.warp.warp_backend.model.event.UrlClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class UrlEventPublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(UrlEventPublisher.class);

  @Autowired
  private KafkaTemplate<String, UrlClickEvent> urlClickEventKafkaTemplate;

  public void publish(UrlClickEvent event) {
    CompletableFuture<SendResult<String, UrlClickEvent>> future =
        urlClickEventKafkaTemplate.send(KafkaTopic.URL_CLICK_EVENTS, event);
    future.handle((sendResult, throwable) -> {
      if (Objects.nonNull(throwable)) {
        LOGGER.warn("[publisher:FAILED] shortUrl={} eventId={}: {}",
            event.getShortUrl(), event.getEventId(), throwable.getMessage());
      } else {
        LOGGER.info("[publisher:ACK] shortUrl={} eventId={} partition={} offset={}",
            event.getShortUrl(), event.getEventId(),
            sendResult.getRecordMetadata().partition(),
            sendResult.getRecordMetadata().offset());
      }
      return null;
    });
  }
}
