package com.warp.warp_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warp.warp_backend.model.constant.KafkaTopic;
import com.warp.warp_backend.model.event.UrlClickEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RedirectListener {

  @Getter
  private static final List<UrlClickEvent> urlClickEvents = new ArrayList<>();

  @Component
  @Slf4j
  public static class Listener {

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopic.URL_CLICK_EVENTS)
    public void on(@Payload UrlClickEvent urlClickEvent) {
      log.info("RedirectListener.onRedirect - " + urlClickEvent);
      urlClickEvents.add(urlClickEvent);
    }
  }
}
