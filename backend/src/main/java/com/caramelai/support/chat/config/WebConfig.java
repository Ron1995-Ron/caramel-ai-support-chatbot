package com.caramelai.support.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the browser app call the API from its dev-server origin. The allowed origins are config. No
 * key or secret is exposed through CORS or any other response.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final ChatProperties properties;

  public WebConfig(ChatProperties properties) {
    this.properties = properties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(properties.corsAllowedOrigins().split(","))
        .allowedMethods("GET", "POST", "PUT")
        .exposedHeaders("X-Request-Id");
  }
}
