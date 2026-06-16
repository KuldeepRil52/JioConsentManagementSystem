package com.example.scanner.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AppConfig {

  @Bean
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(4);
    ex.setMaxPoolSize(8);
    ex.setQueueCapacity(100);
    ex.setThreadNamePrefix("scan-");
    ex.initialize();
    return ex;
  }

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
      ObjectMapper m = new ObjectMapper();

      // Configure JavaTimeModule for proper timestamp handling
      JavaTimeModule timeModule = new JavaTimeModule();
      m.registerModule(timeModule);

      // CRITICAL: Disable timestamps to get ISO-8601 formatted strings
      m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

      m.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);

      // Set specific date format
      m.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));

      // Add duplicate key detection
      m.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);

      // Optional: Other useful security/validation configurations
      m.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
      m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);


      m.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

      // IMPORTANT: Configure serialization inclusion
      m.setDefaultPropertyInclusion(
              com.fasterxml.jackson.annotation.JsonInclude.Value.construct(
                      com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL,
                      com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
              )
      );

      return m;
  }
}