package com.jio.digigov.notification.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.SaslConfigs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Topic configuration for the DPDP Notification Module.
 *
 * This configuration class defines and creates all Kafka topics required for
 * asynchronous notification processing. It ensures proper topic configuration
 * with optimal partition counts, replication factors, and retention policies
 * for different notification types and error handling scenarios.
 *
 * Topic Categories:
 * - Main Topics: Primary notification processing (SMS, Email, Callback)
 * - Dead Letter Queues: Failed message handling and manual recovery
 * - Retry Topics: Delayed retry processing with exponential backoff
 *
 * Configuration Strategy:
 * - Multiple partitions for parallel processing and scalability
 * - Appropriate replication for fault tolerance (adjusted for environment)
 * - Retention policies optimized for notification lifecycle requirements
 * - Compaction disabled for audit trail preservation
 *
 * Partition Strategy:
 * - SMS/Email: 3 partitions for balanced load distribution
 * - Callback: 5 partitions for higher webhook concurrency
 * - DLQ: 1 partition for centralized error processing
 */
@Configuration
@Slf4j
public class KafkaTopicConfig {

    /**
     * Kafka bootstrap servers for admin client configuration.
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * SMS topic name from configuration.
     */
    @Value("${kafka.topics.sms}")
    private String smsTopicName;

    /**
     * Email topic name from configuration.
     */
    @Value("${kafka.topics.email}")
    private String emailTopicName;

    /**
     * Callback topic name from configuration.
     */
    @Value("${kafka.topics.callback}")
    private String callbackTopicName;

    // Topic Configuration Properties
    @Value("${kafka.config.partitions}")
    private int defaultPartitions;

    @Value("${kafka.config.replicas}")
    private int defaultReplicas;

    @Value("${kafka.config.retention-ms}")
    private String defaultRetentionMs;

    @Value("${kafka.config.max-message-bytes}")
    private String maxMessageBytes;

    @Value("${kafka.config.segment-bytes}")
    private String segmentBytes;

    @Value("${kafka.config.min-insync-replicas}")
    private String minInSyncReplicas;

    @Value("${kafka.config.compression-type}")
    private String compressionType;

    @Value("${kafka.config.cleanup-policy}")
    private String cleanupPolicy;

    // // DLQ Configuration Properties
    // @Value("${kafka.config.dlq.partitions}")
    // private int dlqPartitions;

    // @Value("${kafka.config.dlq.retention-ms}")
    // private String dlqRetentionMs;

    // // Retry Configuration Properties
    // @Value("${kafka.config.retry.partitions}")
    // private int retryPartitions;

//    @Value("${kafka.config.retry.retention-ms}")
//    private String retryRetentionMs;

    // Admin Client Configuration Properties
    @Value("${spring.kafka.admin.properties.request.timeout.ms:30000}")
    private int requestTimeoutMs;

    @Value("${spring.kafka.admin.properties.default.api.timeout.ms:60000}")
    private int defaultApiTimeoutMs;

    // Security Configuration Properties
    @Value("${spring.kafka.security.protocol}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.sasl.kerberos.service.name}")
    private String saslKerberosServiceName;

    @Value("${spring.kafka.properties.sasl.jaas.config}")
    private String saslJaasConfig;

    /**
     * Creates and configures the Kafka admin client.
     *
     * The KafkaAdmin bean is responsible for creating and managing Kafka topics
     * programmatically. It ensures that all required topics exist with proper
     * configuration before the application starts processing messages.
     *
     * @return Configured KafkaAdmin for topic management
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, defaultApiTimeoutMs);

        // Security Configuration - Kerberos SASL authentication
        configs.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        configs.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        configs.put(SaslConfigs.SASL_KERBEROS_SERVICE_NAME, saslKerberosServiceName);
        configs.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);

        log.info("KafkaAdmin configured with bootstrap servers: {} and security protocol: {}",
                 bootstrapServers, securityProtocol);
        return new KafkaAdmin(configs);
    }

    /**
     * Creates the SMS notification topic using configurable properties.
     * All settings are driven by application properties for environment-specific configuration.
     *
     * @return Configured SMS topic
     */
    @Bean
    public NewTopic smsNotificationTopic() {
        return TopicBuilder.name(smsTopicName)
                .partitions(defaultPartitions)
                .replicas(defaultReplicas)
                .config("retention.ms", defaultRetentionMs)
                .config("cleanup.policy", cleanupPolicy)
                .config("compression.type", compressionType)
                .config("min.insync.replicas", minInSyncReplicas)
                .config("max.message.bytes", maxMessageBytes)
                .config("segment.bytes", segmentBytes)
                .build();
    }

    /**
     * Creates the Email notification topic using configurable properties.
     * All settings are driven by application properties for environment-specific configuration.
     *
     * @return Configured Email topic
     */
    @Bean
    public NewTopic emailNotificationTopic() {
        return TopicBuilder.name(emailTopicName)
                .partitions(defaultPartitions)
                .replicas(defaultReplicas)
                .config("retention.ms", defaultRetentionMs)
                .config("cleanup.policy", cleanupPolicy)
                .config("compression.type", compressionType)
                .config("min.insync.replicas", minInSyncReplicas)
                .config("max.message.bytes", maxMessageBytes)
                .config("segment.bytes", segmentBytes)
                .build();
    }

    /**
     * Creates the Callback notification topic using configurable properties.
     * All settings are driven by application properties for environment-specific configuration.
     *
     * @return Configured Callback topic
     */
    @Bean
    public NewTopic callbackNotificationTopic() {
        return TopicBuilder.name(callbackTopicName)
                .partitions(defaultPartitions)
                .replicas(defaultReplicas)
                .config("retention.ms", defaultRetentionMs)
                .config("cleanup.policy", cleanupPolicy)
                .config("compression.type", compressionType)
                .config("min.insync.replicas", minInSyncReplicas)
                .config("max.message.bytes", maxMessageBytes)
                .config("segment.bytes", segmentBytes)
                .build();
    }
}