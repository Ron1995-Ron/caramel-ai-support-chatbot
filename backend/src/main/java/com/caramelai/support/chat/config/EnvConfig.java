package com.caramelai.support.chat.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Fails fast at startup when {@code GOOGLE_API_KEY} is missing (Principle II).
 *
 * <p>The key is loaded from the project-root {@code .env} through {@code spring.config.import} in
 * application.yml, or from a real environment variable. This check runs after configuration data is
 * loaded. It reads the key only to test that it is present: the key value is never printed, logged,
 * or placed in an exception message.
 */
public class EnvConfig implements EnvironmentPostProcessor, Ordered {

  static final String KEY_NAME = "GOOGLE_API_KEY";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String key = environment.getProperty(KEY_NAME);
    if (key == null || key.isBlank()) {
      throw new IllegalStateException(
          KEY_NAME
              + " is not set. Create a .env file at the project root containing "
              + KEY_NAME
              + "=<your-key> (see .env.example), or set it as an environment variable.");
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
