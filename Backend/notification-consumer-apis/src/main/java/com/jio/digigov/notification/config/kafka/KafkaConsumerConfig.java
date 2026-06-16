package com.jio.digigov.notification.config.kafka;

import com.jio.digigov.notification.dto.kafka.EventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer configuration for the DPDP Notification Module.
 *
 * This configuration class sets up Kafka consumers for processing asynchronous
 * notifications with optimized settings for reliability, performance, and error
 * handling. It provides separate consumer factories for different notification
 * types to enable fine-tuned processing characteristics.
 *
 * Key Features:
 * - Manual acknowledgment for reliable message processing
 * - Configurable concurrency for different notification types
 * - Comprehensive error handling with retry policies
 * - JSON deserialization with trusted package configuration
 * - Separate consumer factories for SMS, Email, and Callback processing
 *
 * Consumer Groups:
 * - SMS Consumer Group: Optimized for high-throughput SMS processing
 * - Email Consumer Group: Configured for complex email template processing
 * - Callback Consumer Group: Enhanced concurrency for webhook delivery
 *
 * Error Handling:
 * - Fixed backoff retry policy for transient failures
 * - Dead Letter Queue integration for permanent failures
 * - Comprehensive logging for debugging and monitoring
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    private final PartitionAssignmentLogger partitionAssignmentLogger;

    public KafkaConsumerConfig(PartitionAssignmentLogger partitionAssignmentLogger) {
        this.partitionAssignmentLogger = partitionAssignmentLogger;
    }

    /**
     * Kafka bootstrap servers configuration.
     * Can be overridden via KAFKA_BOOTSTRAP_SERVERS environment variable.
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.security.protocol}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.sasl.kerberos.service.name}")
    private String saslKerberosServiceName;

    @Value("${spring.kafka.properties.sasl.jaas.config}")
    private String saslJaasConfig;

    @Value("${spring.kafka.consumer.group-id}")
    private String defaultGroupId;

    @Value("${spring.kafka.listener.sms-concurrency:1}")
    private int smsConcurrency;

    @Value("${spring.kafka.listener.email-concurrency:1}")
    private int emailConcurrency;

    @Value("${spring.kafka.listener.callback-concurrency:1}")
    private int callbackConcurrency;

    // Consumer Configuration Properties
    @Value("${spring.kafka.consumer.enable-auto-commit:false}")
    private boolean enableAutoCommit;

    @Value("${spring.kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.properties.max.poll.records:10}")
    private int maxPollRecords;

    @Value("${spring.kafka.consumer.properties.session.timeout.ms:30000}")
    private int sessionTimeoutMs;

    @Value("${spring.kafka.consumer.properties.heartbeat.interval.ms:10000}")
    private int heartbeatIntervalMs;

    @Value("${spring.kafka.consumer.properties.max.poll.interval.ms:300000}")
    private int maxPollIntervalMs;

    @Value("${spring.kafka.consumer.fetch-max-bytes:52428800}")
    private int fetchMaxBytes;

    @Value("${spring.kafka.consumer.max-partition-fetch-bytes:52428800}")
    private int maxPartitionFetchBytes;

    @Value("${spring.kafka.consumer.fetch-min-bytes:1}")
    private int fetchMinBytes;

    @Value("${spring.kafka.consumer.fetch-max-wait-ms:500}")
    private int fetchMaxWaitMs;

    /**
     * Creates and configures the base Kafka consumer factory.
     *
     * This factory provides the foundation for all notification message consumers
     * with optimized settings for reliability and performance. It uses manual
     * acknowledgment to ensure messages are only marked as processed after
     * successful delivery to external services.
     *
     * Configuration Highlights:
     * - Manual commit mode for reliable processing
     * - JSON deserialization with trusted package security
     * - Optimized polling and session timeout settings
     * - Earliest offset reset for comprehensive message processing
     *
     * @return Configured ConsumerFactory for notification messages
     */
    @Bean
    public ConsumerFactory<String, EventMessage> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Basic Configuration
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Commit Configuration - Manual acknowledgment for reliability
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        // Polling Configuration - Optimize batch processing
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinBytes);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWaitMs);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetchMaxBytes);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, maxPartitionFetchBytes);

        // Session Configuration - Balance responsiveness and stability
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatIntervalMs);

        // Processing Configuration - Control message processing timing
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);

        // JSON Deserialization Configuration - Security and type handling
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.jio.digigov.notification.dto.kafka");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EventMessage.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false); // Disable type headers for security

        // Security Configuration - Kerberos SASL authentication
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        props.put(SaslConfigs.SASL_KERBEROS_SERVICE_NAME, saslKerberosServiceName);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);

        log.info("Kafka Consumer configured with bootstrap servers: {}, group-id: {}, auto-offset-reset: {}, max-poll-records: {}, session-timeout: {}ms",
                bootstrapServers, defaultGroupId, autoOffsetReset, maxPollRecords, sessionTimeoutMs);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates the base Kafka listener container factory.
     *
     * This factory provides common configuration for all notification consumers
     * including manual acknowledgment, error handling, and basic concurrency settings.
     * It serves as the foundation for specialized consumer factories.
     *
     * @return Base configured ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // Manual acknowledgment for reliable processing
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Default concurrency - can be overridden by specific factories
        factory.setConcurrency(3);

        // Error handling with fixed backoff retry
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new FixedBackOff(1000L, 3L) // 3 retries with 1 second delay
        ));

        // Enable detailed logging for debugging
        factory.getContainerProperties().setLogContainerConfig(true);

        // Add partition assignment logger to track rebalancing
        factory.getContainerProperties().setConsumerRebalanceListener(partitionAssignmentLogger);

        log.info("Base KafkaListenerContainerFactory configured successfully");
        return factory;
    }

    /**
     * Creates SMS-specific Kafka listener container factory.
     *
     * Optimized for high-throughput SMS processing with balanced concurrency
     * for DigiGov API calls and database operations. SMS processing is typically
     * fast and can handle higher concurrency levels.
     *
     * @return SMS-optimized ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    @Primary
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> smsKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory = kafkaListenerContainerFactory();

        // Optimized concurrency for SMS processing
        factory.setConcurrency(smsConcurrency);

        // Uses default group ID - consumer groups are scoped per topic
        // factory.getContainerProperties().setGroupId(defaultGroupId + "_SMS");

        log.info("SMS KafkaListenerContainerFactory configured with concurrency: {}, group-id: {}", smsConcurrency, defaultGroupId);
        return factory;
    }

    /**
     * Creates Email-specific Kafka listener container factory.
     *
     * Configured for email processing which may involve complex template rendering,
     * attachment handling, and multiple recipient processing. Uses moderate
     * concurrency to balance throughput with resource usage.
     *
     * @return Email-optimized ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> emailKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory = kafkaListenerContainerFactory();

        // Moderate concurrency for email processing
        factory.setConcurrency(emailConcurrency);

        // Uses default group ID - consumer groups are scoped per topic
        // factory.getContainerProperties().setGroupId(defaultGroupId + "_EMAIL");

        log.info("Email KafkaListenerContainerFactory configured with concurrency: {}, group-id: {}", emailConcurrency, defaultGroupId);
        return factory;
    }

    /**
     * Creates Callback-specific Kafka listener container factory.
     *
     * Optimized for webhook callback processing with higher concurrency to handle
     * multiple concurrent HTTP requests to external systems. Callback processing
     * involves network I/O which can benefit from higher parallelism.
     *
     * @return Callback-optimized ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> callbackKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory = kafkaListenerContainerFactory();

        // Higher concurrency for callback processing (I/O bound)
        factory.setConcurrency(callbackConcurrency);

        // Uses default group ID - consumer groups are scoped per topic
        // factory.getContainerProperties().setGroupId(defaultGroupId + "_CALLBACK");

        // Extended timeout for webhook processing
        factory.getContainerProperties().setPollTimeout(5000L);

        log.info("Callback KafkaListenerContainerFactory configured with concurrency: {}, group-id: {}", callbackConcurrency, defaultGroupId);
        return factory;
    }

    // /**
    //  * Creates Dead Letter Queue (DLQ) consumer factory.
    //  *
    //  * Specialized factory for processing failed messages from DLQ topics.
    //  * Uses single-threaded processing for careful handling of problematic
    //  * messages and detailed error analysis.
    //  *
    //  * @return DLQ-specific ConcurrentKafkaListenerContainerFactory
    //  */
    // @Bean
    // public ConcurrentKafkaListenerContainerFactory<String, EventMessage> dlqKafkaListenerContainerFactory() {
    //     ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory = kafkaListenerContainerFactory();

    //     // Single-threaded processing for DLQ messages
    //     factory.setConcurrency(1);

    //     // DLQ-specific group ID
    //     factory.getContainerProperties().setGroupId(defaultGroupId + "_DLQ");

    //     // No retry for DLQ messages - they have already failed multiple times
    //     factory.setCommonErrorHandler(new DefaultErrorHandler(
    //         new FixedBackOff(0L, 0L) // No retries for DLQ
    //     ));

    //     log.info("DLQ KafkaListenerContainerFactory configured with concurrency: 1");
    //     return factory;
    // }
}