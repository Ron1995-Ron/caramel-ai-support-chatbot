package com.caramelai.support.chat.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code caramel.chat}. See application.yml for the defaults and their source. */
@ConfigurationProperties(prefix = "caramel.chat")
public record ChatProperties(
    int maxMessageLength,
    Duration sessionIdleTimeout,
    int historyTokenBudget,
    Duration providerTimeout,
    String supportContentLocation,
    String corsAllowedOrigins) {}
