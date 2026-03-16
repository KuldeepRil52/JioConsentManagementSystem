package com.jio.digigov.notification.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer configuration for the DPDP Notification Module.
 *
 * This configuration class sets up Kafka producers for asynchronous notification
 * processing with optimized settings for reliability, performance, and monitoring.
 * It implements best practices for enterprise-grade Kafka messaging including
 * idempotence, compression, and comprehensive error handling.
 *
 * Key Features:
 * - Idempotent producers to prevent message duplication
 * - Snappy compression for optimal network utilization
 * - Configurable batch processing for throughput optimization
 * - Comprehensive timeout settings for reliability
 * - Producer listener for monitoring and debugging
 *
 * Reliability Settings:
 * - acks=all: Wait for all in-sync replicas to acknowledge
 * - retries=3: Automatic retry on transient failures
 * - idempotence=true: Prevent duplicate messages
 * - delivery.timeout=120s: Maximum time for message delivery
 *
 * Performance Settings:
 * - batch.size=32KB: Optimal batch size for throughput
 * - linger.ms=10: Small delay to allow batching
 * - compression=snappy: Fast compression with good ratio
 * - request.timeout=30s: Per-request timeout
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaProducerConfig {

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

    /**
     * Creates and configures the Kafka producer factory.
     *
     * This factory is responsible for creating Kafka producer instances with
     * optimized settings for the notification service. The configuration balances
     * reliability, performance, and resource utilization for high-throughput
     * notification processing.
     *
     * Configuration Highlights:
     * - String keys for partition routing and message identification
     * - JSON serialization for complex message payloads
     * - Idempotent producers to ensure exactly-once semantics
     * - Optimized batching and compression for network efficiency
     *
     * @return Configured ProducerFactory for notification messages
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic Configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability Settings - Ensure message delivery and prevent duplicates
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all in-sync replicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry failed sends
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicates

        // Performance Settings - Optimize throughput and network usage
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768); // 32KB batch size
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Fast compression
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Allow small batching delay

        // Timeout Settings - Control delivery timing and failure detection
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 30 second request timeout
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 2 minute delivery timeout

        // Buffer Settings - Manage memory usage and blocking behavior
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000); // 1 minute max block time

        // Connection Settings - Optimize network connections
        configProps.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000); // 9 minute idle timeout
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // Max concurrent requests

        // Security Configuration - Kerberos SASL authentication
        configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        configProps.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        configProps.put(SaslConfigs.SASL_KERBEROS_SERVICE_NAME, saslKerberosServiceName);
        configProps.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);

        log.info("Kafka Producer configured with bootstrap servers: {}", bootstrapServers);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates and configures the Kafka template for message publishing.
     *
     * The KafkaTemplate provides a high-level abstraction for sending messages
     * to Kafka topics. It includes producer listeners for monitoring and debugging
     * message delivery status, enabling comprehensive tracking of notification
     * processing performance.
     *
     * Features:
     * - Asynchronous message sending with callback support
     * - Integrated producer listeners for success and failure tracking
     * - Automatic serialization of complex notification objects
     * - Transaction support for exactly-once processing (if needed)
     *
     * @return Configured KafkaTemplate for notification publishing
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());

        // Add producer listener for monitoring and debugging
        template.setProducerListener(new ProducerListener<String, Object>() {
            /**
             * Called when a message is successfully sent to Kafka.
             * Logs success information for monitoring and debugging.
             */
            @Override
            public void onSuccess(ProducerRecord<String, Object> producerRecord, RecordMetadata recordMetadata) {
                log.debug("Message sent successfully to topic {} partition {} offset {} with key {}",
                    recordMetadata.topic(),
                    recordMetadata.partition(),
                    recordMetadata.offset(),
                    producerRecord.key());
            }

            /**
             * Called when a message fails to be sent to Kafka.
             * Logs error information for debugging and alerting.
             */
            @Override
            public void onError(ProducerRecord<String, Object> producerRecord,
                    RecordMetadata recordMetadata, Exception exception) {
                log.error("Failed to send message to topic {} with key {}: {}",
                    producerRecord.topic(),
                    producerRecord.key(),
                    exception.getMessage(), exception);
            }
        });

        log.info("KafkaTemplate configured successfully with producer listeners");
        return template;
    }
}