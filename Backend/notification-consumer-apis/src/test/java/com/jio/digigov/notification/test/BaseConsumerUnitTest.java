package com.jio.digigov.notification.test;

import com.jio.digigov.notification.util.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base unit test class for Kafka consumer unit testing.
 *
 * Features:
 * - Mockito extension integration
 * - Tenant context management
 * - Consumer-specific test utilities
 * - Fast execution without Spring context
 *
 * Usage:
 * Extend this class for unit tests that focus on testing individual
 * consumer components in isolation.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseConsumerUnitTest {

    protected static final String TEST_TENANT_ID = "test-tenant";
    protected static final String TEST_BUSINESS_ID = "test-business";
    protected static final String TEST_CORRELATION_ID = "CORR-TEST123456789012";
    protected static final String TEST_REQUEST_ID = "REQ-TEST123456789012";
    protected static final String TEST_TEMPLATE_ID = "TEMPL0000000001";

    // Test message formats
    protected static final String NOTIFICATION_CALLBACK_MESSAGE_FORMAT =
            "{\"templateId\":\"%s\",\"status\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}";

    protected static final String OTP_CALLBACK_MESSAGE_FORMAT =
            "{\"requestId\":\"%s\",\"status\":\"%s\",\"otp\":\"%s\",\"timestamp\":\"%s\"}";

    @BeforeEach
    void setUpBaseConsumerUnitTest() {
        setupTenantContext();
    }

    @AfterEach
    void tearDownBaseConsumerUnitTest() {
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

    protected String getTestRequestId() {
        return TEST_REQUEST_ID;
    }

    protected String getTestTemplateId() {
        return TEST_TEMPLATE_ID;
    }

    /**
     * Create notification callback message
     */
    protected String createNotificationCallbackMessage(String templateId, String status, String message) {
        return String.format(NOTIFICATION_CALLBACK_MESSAGE_FORMAT,
                templateId, status, message, java.time.Instant.now());
    }

    /**
     * Create OTP callback message
     */
    protected String createOtpCallbackMessage(String requestId, String status, String otp) {
        return String.format(OTP_CALLBACK_MESSAGE_FORMAT,
                requestId, status, otp, java.time.Instant.now());
    }

    /**
     * Create test template callback message with default values
     */
    protected String createTestTemplateCallback() {
        return createNotificationCallbackMessage(TEST_TEMPLATE_ID, "SUCCESS", "Template processed successfully");
    }

    /**
     * Create test OTP callback message with default values
     */
    protected String createTestOtpCallback() {
        return createOtpCallbackMessage(TEST_REQUEST_ID, "SUCCESS", "123456");
    }

    /**
     * Create failed notification callback message
     */
    protected String createFailedNotificationCallback(String templateId, String errorMessage) {
        return createNotificationCallbackMessage(templateId, "FAILED", errorMessage);
    }

    /**
     * Create failed OTP callback message
     */
    protected String createFailedOtpCallback(String requestId, String errorMessage) {
        return createOtpCallbackMessage(requestId, "FAILED", errorMessage);
    }
}