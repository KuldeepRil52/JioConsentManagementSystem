package com.jio.digigov.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration validation component for validating application startup configuration.
 *
 * This component validates all critical configuration properties at application startup
 * to ensure the application has all required settings properly configured before
 * accepting traffic. It prevents runtime errors due to missing or invalid configuration.
 *
 * Validation Categories:
 * - Database configuration (MongoDB connection)
 * - Kafka configuration (brokers, topics)
 * - Cache configuration (type, properties)
 * - Security configuration (SSL, authentication)
 * - External API configuration (DigiGov endpoints)
 * - Logging and monitoring configuration
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class ConfigurationValidation implements ApplicationListener<ApplicationReadyEvent> {

    // Database configuration
    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:}")
    private String mongoDatabase;

    // Kafka configuration
    @Value("${spring.kafka.bootstrap-servers:}")
    private String kafkaBootstrapServers;

    @Value("${kafka.topics.sms:}")
    private String smsTopicName;

    @Value("${kafka.topics.email:}")
    private String emailTopicName;

    @Value("${kafka.topics.callback:}")
    private String callbackTopicName;

    // Cache configuration
    @Value("${cache.type:}")
    private String cacheType;

    @Value("${cache.enabled:true}")
    private boolean cacheEnabled;

    // Security configuration
    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${management.endpoints.web.exposure.include:}")
    private String actuatorEndpoints;

    // External API configuration is retrieved dynamically from ngConfiguration per business

    // Application configuration
    @Value("${spring.application.name:}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${logging.file.path:}")
    private String logFilePath;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Starting configuration validation for application: {}", applicationName);

        List<String> validationErrors = new ArrayList<>();

        // Validate database configuration
        validateDatabaseConfiguration(validationErrors);

        // Validate Kafka configuration
        validateKafkaConfiguration(validationErrors);

        // Validate cache configuration
        validateCacheConfiguration(validationErrors);

        // Validate security configuration
        validateSecurityConfiguration(validationErrors);

        // External API configuration (DigiGov) is retrieved dynamically from ngConfiguration per business
        // so no static validation is required

        // Validate logging configuration
        validateLoggingConfiguration(validationErrors);

        // Report validation results
        if (validationErrors.isEmpty()) {
            log.info("✅ Configuration validation completed successfully");
            log.info("Application is ready to accept traffic on port: {}", serverPort);
            logConfigurationSummary();
        } else {
            log.error("❌ Configuration validation failed with {} errors:", validationErrors.size());
            validationErrors.forEach(error -> log.error("  - {}", error));
            throw new IllegalStateException(
                "Application configuration validation failed. Please check the configuration and restart.");
        }
    }

    private void validateDatabaseConfiguration(List<String> errors) {
        log.debug("Validating database configuration...");

        if (mongoUri == null || mongoUri.trim().isEmpty()) {
            errors.add("MongoDB URI is not configured (spring.data.mongodb.uri)");
        }

        if (mongoDatabase == null || mongoDatabase.trim().isEmpty()) {
            errors.add("MongoDB database name is not configured (spring.data.mongodb.database)");
        }

        if (errors.isEmpty()) {
            log.info("✅ Database configuration validated");
        }
    }

    private void validateKafkaConfiguration(List<String> errors) {
        log.debug("Validating Kafka configuration...");

        if (kafkaBootstrapServers == null || kafkaBootstrapServers.trim().isEmpty()) {
            errors.add("Kafka bootstrap servers not configured (spring.kafka.bootstrap-servers)");
        }

        if (smsTopicName == null || smsTopicName.trim().isEmpty()) {
            errors.add("SMS topic name not configured (kafka.topics.sms)");
        }

        if (emailTopicName == null || emailTopicName.trim().isEmpty()) {
            errors.add("Email topic name not configured (kafka.topics.email)");
        }

        if (callbackTopicName == null || callbackTopicName.trim().isEmpty()) {
            errors.add("Callback topic name not configured (kafka.topics.callback)");
        }

        if (errors.isEmpty()) {
            log.info("✅ Kafka configuration validated");
        }
    }

    private void validateCacheConfiguration(List<String> errors) {
        log.debug("Validating cache configuration...");

        if (cacheEnabled) {
            if (cacheType == null || cacheType.trim().isEmpty()) {
                errors.add("Cache type not specified (cache.type) but caching is enabled");
            } else if (!cacheType.equalsIgnoreCase("caffeine") && !cacheType.equalsIgnoreCase("mongodb")) {
                errors.add("Invalid cache type: " + cacheType + ". Supported types: caffeine, mongodb");
            }
        }

        if (errors.isEmpty()) {
            log.info("✅ Cache configuration validated (type: {}, enabled: {})", cacheType, cacheEnabled);
        }
    }

    private void validateSecurityConfiguration(List<String> errors) {
        log.debug("Validating security configuration...");

        if (actuatorEndpoints == null || actuatorEndpoints.trim().isEmpty()) {
            log.warn("⚠️  Actuator endpoints not explicitly configured - using defaults");
        }

        // Check if SSL is properly configured for production
        if (sslEnabled) {
            log.info("✅ SSL enabled for secure communication");
        } else {
            log.warn("⚠️  SSL is disabled - ensure this is intended for your environment");
        }
    }


    private void validateLoggingConfiguration(List<String> errors) {
        log.debug("Validating logging configuration...");

        if (logFilePath == null || logFilePath.trim().isEmpty()) {
            log.warn("⚠️  Log file path not configured - using default location");
        }

        log.info("✅ Logging configuration validated");
    }

    private void logConfigurationSummary() {
        log.info("=== Configuration Summary ===");
        log.info("Application: {}", applicationName);
        log.info("Server Port: {}", serverPort);
        log.info("MongoDB Database: {}", mongoDatabase);
        log.info("Kafka Bootstrap Servers: {}", kafkaBootstrapServers);
        log.info("Cache Type: {} (enabled: {})", cacheType, cacheEnabled);
        log.info("SSL Enabled: {}", sslEnabled);
        log.info("Log File Path: {}", logFilePath != null && !logFilePath.trim().isEmpty() ? logFilePath : "default");
        log.info("===========================");
    }
}