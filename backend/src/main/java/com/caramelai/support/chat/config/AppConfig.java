package com.caramelai.support.chat.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatProperties.class)
public class AppConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  /** In-process registry for the success metrics. Nothing is exported or exposed over HTTP. */
  @Bean
  @ConditionalOnMissingBean
  MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }
}
