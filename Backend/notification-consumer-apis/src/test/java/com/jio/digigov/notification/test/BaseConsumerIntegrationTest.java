package com.jio.digigov.notification.test;

import com.jio.digigov.notification.util.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base integration test class for Kafka consumer testing.
 *
 * Features:
 * - Embedded Kafka integration
 * - MongoDB test container integration
 * - Tenant context management
 * - Test profile activation
 * - Consumer-specific test utilities
 *
 * Usage:
 * Extend this class for integration tests that require Kafka consumers
 * and full Spring context.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    },
    topics = {
        "notification-callback-topic",
        "otp-callback-topic",
        "template-status-topic",
        "audit-topic"
    }
)
@DirtiesContext
public abstract class BaseConsumerIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4.6");

    protected static final String TEST_TENANT_ID = "test-tenant";
    protected static final String TEST_BUSINESS_ID = "test-business";
    protected static final String TEST_CORRELATION_ID = "CORR-TEST123456789012";

    // Kafka topic names
    protected static final String NOTIFICATION_CALLBACK_TOPIC = "notification-callback-topic";
    protected static final String OTP_CALLBACK_TOPIC = "otp-callback-topic";
    protected static final String TEMPLATE_STATUS_TOPIC = "template-status-topic";
    protected static final String AUDIT_TOPIC = "audit-topic";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.kafka.producer.bootstrap-servers", () -> "localhost:9092");

        // Kafka topic configurations
        registry.add("kafka.topics.notification-callback", () -> NOTIFICATION_CALLBACK_TOPIC);
        registry.add("kafka.topics.otp-callback", () -> OTP_CALLBACK_TOPIC);
        registry.add("kafka.topics.template-status", () -> TEMPLATE_STATUS_TOPIC);
        registry.add("kafka.topics.audit", () -> AUDIT_TOPIC);
    }

    @BeforeEach
    void setUpBaseConsumerTest() {
        setupTenantContext();
    }

    @AfterEach
    void tearDownBaseConsumerTest() {
        cleanupTenantContext();
    }

    protected void setupTenantContext() {
        TenantContextHolder.setTenant(TEST_TENANT_ID);
        TenantContextHolder.setBusinessId(TEST_BUSINESS_ID);
    }

    protected void setupTenantContext(String tenantId, String businessId) {
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);
    }

    protected void cleanupTenantContext() {
        TenantContextHolder.clear();
    }

    protected String getTestTenantId() {
        return TEST_TENANT_ID;
    }

    protected String getTestBusinessId() {
        return TEST_BUSINESS_ID;
    }

    protected String getTestCorrelationId() {
        return TEST_CORRELATION_ID;
    }

    protected String getNotificationCallbackTopic() {
        return NOTIFICATION_CALLBACK_TOPIC;
    }

    protected String getOtpCallbackTopic() {
        return OTP_CALLBACK_TOPIC;
    }

    protected String getTemplateStatusTopic() {
        return TEMPLATE_STATUS_TOPIC;
    }

    protected String getAuditTopic() {
        return AUDIT_TOPIC;
    }

    /**
     * Wait for Kafka consumer to process messages
     */
    protected void waitForKafkaProcessing() {
        try {
            Thread.sleep(2000); // Wait 2 seconds for message processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted while waiting for Kafka processing", e);
        }
    }

    /**
     * Wait for custom duration
     */
    protected void waitForProcessing(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted while waiting for processing", e);
        }
    }
}