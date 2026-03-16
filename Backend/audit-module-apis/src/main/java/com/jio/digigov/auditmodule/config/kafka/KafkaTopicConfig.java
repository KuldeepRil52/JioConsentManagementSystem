package com.jio.digigov.auditmodule.config.kafka;

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

@Configuration
@Slf4j
public class KafkaTopicConfig {

    /**
     * Kafka bootstrap servers for admin client configuration.
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.audit}")
    private String auditTopic;

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
     * Creates the Audit topic using configurable properties.
     * All settings are driven by application properties for environment-specific configuration.
     *
     * @return Configured Audit topic
     */
    @Bean
    public NewTopic auditTopic() {
        log.info("Creating Kafka topic for audits: {}", auditTopic);
        return TopicBuilder.name(auditTopic)
                .partitions(defaultPartitions)
                .replicas(defaultReplicas)      // ensure fault tolerance
                .config("retention.ms", defaultRetentionMs)
                .config("cleanup.policy", cleanupPolicy)
                .config("compression.type", compressionType)
                .config("min.insync.replicas", minInSyncReplicas)
                .config("max.message.bytes", maxMessageBytes)
                .config("segment.bytes", segmentBytes)
                .build();
    }

}