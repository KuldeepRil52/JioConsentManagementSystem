package com.jio.digigov.notification.service.kafka.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.kafka.EventMessage;
import com.jio.digigov.notification.dto.kafka.EmailPayload;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.entity.notification.NotificationEmail;
import com.jio.digigov.notification.enums.CredentialType;
import com.jio.digigov.notification.enums.ErrorCode;
import com.jio.digigov.notification.enums.ErrorMessage;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.dto.provider.ProviderEmailRequest;
import com.jio.digigov.notification.dto.provider.ProviderEmailResponse;
import com.jio.digigov.notification.service.provider.NotificationProviderFactory;
import com.jio.digigov.notification.service.provider.NotificationProviderService;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.exception.BusinessException;
import com.jio.digigov.notification.repository.event.EventConfigurationRepository;
import com.jio.digigov.notification.repository.notification.NotificationEmailRepository;
import com.jio.digigov.notification.service.DigiGovClientService;
import com.jio.digigov.notification.service.TokenService;
import com.jio.digigov.notification.service.audit.AuditEventService;
import com.jio.digigov.notification.service.kafka.RetryPolicyService;
import com.jio.digigov.notification.service.template.TemplateService;
import com.jio.digigov.notification.service.masterlist.MasterListService;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import com.jio.digigov.notification.util.TenantContextHolder;
import com.jio.digigov.notification.context.EventContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Kafka consumer for processing Email notification messages.
 *
 * This service listens to the email notification topic and processes messages by:
 * 1. Resolving email templates (subject and body) with dynamic arguments
 * 2. Integrating with DigiGov Email API for delivery
 * 3. Handling HTML/text email formatting and attachments
 * 4. Updating notification status in tenant-specific databases
 * 5. Implementing retry logic with exponential backoff
 * 6. Supporting bulk email operations for efficiency
 *
 * Performance Features:
 * - Template caching for improved rendering performance
 * - Parallel processing with configurable concurrency
 * - Token caching for DigiGov API authentication
 * - Manual acknowledgment for reliable message processing
 * - Circuit breaker pattern for external API resilience
 * - Email queue management for rate limiting compliance
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationConsumer {

    private final NotificationEmailRepository emailRepository;
    private final EventConfigurationRepository eventConfigurationRepository;
    private final TemplateService templateService;
    private final RetryPolicyService retryPolicyService;
    private final MongoTemplateProvider mongoTemplateProvider;
    private final DigiGovClientService digiGovClientService;
    private final NotificationProviderFactory providerFactory;
    private final TokenService tokenService;
    private final MasterListService masterListService;
    private final ObjectMapper objectMapper;
    private final AuditEventService auditEventService;

    @Value("${notification.email.send-on-missing-config:true}")
    private boolean sendOnMissingConfig;

    @Value("${email.max-body-size:1048576}") // 1MB default
    private int maxEmailBodySize;

    /**
     * SecureRandom instance for cryptographically secure notification ID generation
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Main entry point for email event processing.
     * Processes event messages with validation, notification creation, and delivery.
     */
    @KafkaListener(
        topics = "${kafka.topics.email}",
        groupId = "${kafka.consumer.group-id}",
        containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void processEmailNotification(
            @Payload EventMessage eventMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Processing email event: eventId={}, messageId={}, topic={}, partition={}, offset={}",
                eventMessage.getEventId(), eventMessage.getMessageId(), topic, partition, offset);

        String notificationId = null;
        try {
            // Check if this consumer should process this event
            if (!shouldProcessEvent(eventMessage)) {
                log.debug("Email consumer skipping event - customer identifier type: {}",
                         eventMessage.getCustomerIdentifiers().getType());
                acknowledgment.acknowledge();
                return;
            }

            // Get tenant-specific MongoTemplate
            var mongoTemplate = mongoTemplateProvider.getTemplate(eventMessage.getTenantId());

            // Validate event configuration
            boolean shouldSend = validateEventConfigurationAndChannelEnabled(eventMessage, mongoTemplate);
            if (!shouldSend) {
                log.info("Email not enabled for event or missing config - eventId: {}", eventMessage.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // Generate notification ID and create notification record
            notificationId = generateNotificationId(NotificationChannel.EMAIL.name());
            NotificationEmail emailNotification = createEmailNotificationRecord(eventMessage, notificationId, mongoTemplate);
            if (emailNotification == null) {
                log.error("Failed to create Email notification record - eventId: {}", eventMessage.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // Update status to PROCESSING
            emailRepository.updateStatus(notificationId, NotificationStatus.PROCESSING.name(), mongoTemplate);

            // Process the email notification
            processEmail(emailNotification, eventMessage, mongoTemplate);

            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.info("Successfully processed email event: eventId={}, notificationId={}",
                    eventMessage.getEventId(), notificationId);

        } catch (Exception e) {
            log.error("Error processing email event: eventId={}, notificationId={}, error={}",
                     eventMessage.getEventId(), notificationId, e.getMessage(), e);

            handleProcessingError(notificationId, eventMessage.getTenantId(), e, acknowledgment);
        }
    }

    /**
     * Core email processing logic with provider abstraction (DigiGov or SMTP).
     */
    private void processEmail(NotificationEmail notification, EventMessage eventMessage,
                             MongoTemplate mongoTemplate) {

        try {
            // Validate email addresses
            validateEmailAddresses(notification.getToEmail(), notification.getCc(), null);

            // Get template to determine provider type
            NotificationTemplate template = getTemplate(notification.getTemplateId(), mongoTemplate);


            // Get NotificationConfig
            NotificationConfig notificationConfig = getNotificationConfig(
                    eventMessage.getBusinessId(), mongoTemplate);

            ProviderType providerType = notificationConfig.getProviderType() != null ?
                    notificationConfig.getProviderType() : ProviderType.DIGIGOV;

            log.info("Processing email via provider: {}, templateId: {}, notificationId: {}",
                    providerType, notification.getTemplateId(), notification.getNotificationId());

            // Get appropriate provider
            NotificationProviderService provider = providerFactory.getProvider(
                    providerType,
                    NotificationChannel.EMAIL,
                    notificationConfig
            );

            // Get resolved arguments from Master List Resolution
            Map<String, Map<String, String>> resolvedEmailArgs = resolveEmailArgumentsWithMasterList(
                    notification.getTemplateId(), eventMessage);

            Map<String, String> resolvedSubjectArgs = resolvedEmailArgs.get("subject");
            Map<String, String> resolvedBodyArgs = resolvedEmailArgs.get("body");

            // Build provider email request
            ProviderEmailRequest providerRequest = buildProviderEmailRequest(
                    notification,
                    template,
                    resolvedSubjectArgs,
                    resolvedBodyArgs,
                    eventMessage,
                    providerType
            );

            // Send email via provider
            ProviderEmailResponse response = provider.sendEmail(providerRequest, notificationConfig);

            // Handle the response
            if (response.getSuccess()) {
                // Update notification status to SENT
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("messageId", response.getMessageId());
                responseMap.put("message", response.getMessage());
                responseMap.put("provider", providerType.name());
                responseMap.put("timestamp", response.getTimestamp());

                emailRepository.updateDeliveryInfo(
                        notification.getNotificationId(),
                        NotificationStatus.SENT.name(),
                        LocalDateTime.now(),
                        responseMap,
                        mongoTemplate
                );

                // Record success metrics
                retryPolicyService.recordRetryMetrics(
                        NotificationChannel.EMAIL.name(),
                        notification.getAttemptCount(),
                        true,
                        null
                );

                // Audit successful delivery
                auditEventService.auditNotificationDelivery(
                        notification.getNotificationId(),
                        "DELIVERY_SUCCESS",
                        "EMAIL_CONSUMER",
                        eventMessage.getTenantId(),
                        eventMessage.getBusinessId(),
                        eventMessage.getTransactionId(),
                        eventMessage.getSourceIp(),
                        eventMessage.getEventId()
                );

                log.info("Email sent successfully via {}: notificationId={}, messageId={}, attempts={}",
                        providerType, notification.getNotificationId(),
                        response.getMessageId(), notification.getAttemptCount());
            } else {
                log.error("Email delivery failed via {}: notificationId={}, error={}",
                        providerType, notification.getNotificationId(), response.getErrorDescription());

                // Audit failed delivery
                auditEventService.auditNotificationDelivery(
                        notification.getNotificationId(),
                        "DELIVERY_FAILED",
                        "EMAIL_CONSUMER",
                        eventMessage.getTenantId(),
                        eventMessage.getBusinessId(),
                        eventMessage.getTransactionId(),
                        eventMessage.getSourceIp(),
                        eventMessage.getEventId()
                );

                handleDeliveryFailure(notification, response.getErrorDescription(), mongoTemplate);
            }

        } catch (Exception e) {
            log.error("Error processing email for notificationId={}: {}",
                     notification.getNotificationId(), e.getMessage(), e);

            // Audit failed delivery
            auditEventService.auditNotificationDelivery(
                    notification.getNotificationId(),
                    "DELIVERY_FAILED",
                    "EMAIL_CONSUMER",
                    eventMessage.getTenantId(),
                    eventMessage.getBusinessId(),
                    eventMessage.getTransactionId(),
                    eventMessage.getSourceIp(),
                    eventMessage.getEventId()
            );

            handleDeliveryFailure(notification, e.getMessage(), mongoTemplate);
        }
    }

    /**
     * Build provider email request from notification and template.
     */
    private ProviderEmailRequest buildProviderEmailRequest(
            NotificationEmail notification,
            NotificationTemplate template,
            Map<String, String> resolvedSubjectArgs,
            Map<String, String> resolvedBodyArgs,
            EventMessage eventMessage,
            ProviderType configProviderType) {

        ProviderEmailRequest.ProviderEmailRequestBuilder builder = ProviderEmailRequest.builder()
                .templateId(notification.getTemplateId())
                .to(List.of(notification.getToEmail()))
                .cc(notification.getCc())
                .transactionId(eventMessage.getTransactionId())
                .tenantId(eventMessage.getTenantId())
                .businessId(eventMessage.getBusinessId())
                .eventId(eventMessage.getEventId())
                .notificationId(notification.getNotificationId())
                .messageDetails("Email notification");

        // For SMTP configuration, resolve template locally (works with both SMTP and DigiGov templates)
        if (configProviderType == ProviderType.SMTP && template.getEmailConfig() != null) {
            var emailConfig = template.getEmailConfig();

            // Resolve subject - replace all placeholders with resolved values
            String subject = resolveTemplateText(
                    emailConfig.getTemplateSubject(),
                    resolvedSubjectArgs != null ? resolvedSubjectArgs : new HashMap<>()
            );

            // Resolve body - replace all placeholders with resolved values
            String body = resolveTemplateText(
                    emailConfig.getTemplateBody(),
                    resolvedBodyArgs != null ? resolvedBodyArgs : new HashMap<>()
            );

            builder.subject(subject)
                    .body(body)
                    .emailType(emailConfig.getEmailType())
                    .fromName(emailConfig.getTemplateFromName());

            log.debug("SMTP config: Resolved email subject and body locally for templateId: {}", notification.getTemplateId());
        } else {
            // For DigiGov configuration, pass arguments as-is (DigiGov API resolves them)
            builder.subjectArguments(resolvedSubjectArgs != null ? resolvedSubjectArgs : new HashMap<>())
                    .bodyArguments(resolvedBodyArgs != null ? resolvedBodyArgs : new HashMap<>());

            log.debug("DigiGov config: Passing arguments to DigiGov API for templateId: {}", notification.getTemplateId());
        }

        return builder.build();
    }

    /**
     * Resolve template text by replacing placeholders with actual values.
     */
    private String resolveTemplateText(String template, Map<String, String> arguments) {
        if (template == null) {
            return "";
        }

        String result = template;
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Get NotificationConfig for the business with 3-step fallback logic.
     *
     * Step 1: Try to find by businessId directly
     * Step 2: If not found, try to find by scopeLevel=TENANT (tenant-wide config)
     * Step 3: If still not found, try using tenantId as businessId
     */
    private NotificationConfig getNotificationConfig(String businessId, MongoTemplate mongoTemplate) {
        String tenantId = TenantContextHolder.getTenant();

        log.debug("Finding configuration for businessId: {}, tenantId: {}", businessId, tenantId);

        // Step 1: Try to find by businessId
        Query step1Query = new Query(Criteria.where("businessId").is(businessId));
        NotificationConfig config = mongoTemplate.findOne(
                step1Query,
                NotificationConfig.class,
                "notification_configurations"
        );

        if (config != null) {
            log.debug("Configuration found for businessId: {} (Step 1: direct match)", businessId);
            return config;
        }

        // Step 2: If not found, try to find by scopeLevel = TENANT
        log.debug("Configuration not found for businessId: {}, trying scopeLevel=TENANT fallback (Step 2)", businessId);
        Query step2Query = new Query(Criteria.where("scopeLevel").is("TENANT"));
        config = mongoTemplate.findOne(
                step2Query,
                NotificationConfig.class,
                "notification_configurations"
        );

        if (config != null) {
            log.info("Configuration found using TENANT-level fallback (Step 2: scopeLevel=TENANT match): tenantId={}", tenantId);
            return config;
        }

        // Step 3: If still not found, try to find by tenantId in businessId field
        log.debug("Configuration not found for scopeLevel=TENANT, trying tenantId as businessId fallback (Step 3)");
        Query step3Query = new Query(Criteria.where("businessId").is(tenantId));
        config = mongoTemplate.findOne(
                step3Query,
                NotificationConfig.class,
                "notification_configurations"
        );

        if (config != null) {
            log.info("Configuration found using tenantId as businessId fallback (Step 3: tenantId match): tenantId={}", tenantId);
            return config;
        }

        // If all three steps fail, throw exception
        log.error("Configuration not found after all 3 fallback steps for businessId: {}, tenantId: {}", businessId, tenantId);
        throw new BusinessException("CONFIG_NOT_FOUND",
                "NotificationConfig not found for businessId: " + businessId + " after all fallback attempts");
    }

    /**
     * Get template by templateId.
     */
    private NotificationTemplate getTemplate(String templateId, MongoTemplate mongoTemplate) {
        Query query = new Query(Criteria.where("templateId").is(templateId));
        NotificationTemplate template = mongoTemplate.findOne(
                query,
                NotificationTemplate.class,
                "notification_templates"
        );

        if (template == null) {
            throw new BusinessException("TEMPLATE_NOT_FOUND",
                    "Template not found for templateId: " + templateId);
        }

        return template;
    }

    /**
     * Validates email addresses for proper format and domain restrictions.
     */
    private void validateEmailAddresses(String toEmail, List<String> ccEmails, List<String> bccEmails) {
        // Basic email validation - could be enhanced with more sophisticated validation
        validateEmail(toEmail);

        if (ccEmails != null) {
            ccEmails.forEach(this::validateEmail);
        }

        if (bccEmails != null) {
            bccEmails.forEach(this::validateEmail);
        }
    }

    private void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
    }

    /**
     * Handles email delivery failures with retry logic.
     */
    private void handleDeliveryFailure(NotificationEmail notification, String errorMessage,
                                     MongoTemplate mongoTemplate) {
        try {
            int currentAttempt = notification.getAttemptCount();
            String channel = NotificationChannel.EMAIL.name();

            // Check if error is retryable
            if (!retryPolicyService.isRetryableError(errorMessage, channel)) {
                log.warn("Email delivery failed with non-retryable error: notificationId={}, error={}",
                        notification.getNotificationId(), errorMessage);

                // Mark as permanently failed for non-retryable errors
                emailRepository.updateStatus(notification.getNotificationId(), NotificationStatus.FAILED.name(), mongoTemplate);
                retryPolicyService.recordRetryMetrics(channel, currentAttempt, false, "NON_RETRYABLE");
                return;
            }

            // Check if we should retry based on attempt count
            if (retryPolicyService.shouldRetry(currentAttempt, channel)) {
                // Calculate next retry time with exponential backoff
                LocalDateTime nextRetryAt = retryPolicyService.calculateNextRetry(currentAttempt, channel);

                // Update retry information
                emailRepository.updateRetryInfo(
                        notification.getNotificationId(),
                        currentAttempt + 1,
                        nextRetryAt,
                        errorMessage,
                        mongoTemplate
                );

                // Update status to RETRY_SCHEDULED
                emailRepository.updateStatus(notification.getNotificationId(), "RETRY_SCHEDULED", mongoTemplate);

                retryPolicyService.recordRetryMetrics(channel, currentAttempt, false, "RETRYABLE");

                log.warn("Email delivery failed, scheduling retry: notificationId={}, attempt={}, nextRetry={}, error={}",
                        notification.getNotificationId(), currentAttempt + 1, nextRetryAt, errorMessage);
            } else {
                // Mark as permanently failed after max attempts
                emailRepository.updateStatus(notification.getNotificationId(), NotificationStatus.FAILED.name(), mongoTemplate);

                retryPolicyService.recordRetryMetrics(channel, currentAttempt, false, "MAX_ATTEMPTS_EXCEEDED");

                log.error("Email delivery permanently failed after {} attempts: notificationId={}, finalError={}",
                         retryPolicyService.getMaxRetryAttempts(channel), notification.getNotificationId(), errorMessage);
            }

        } catch (Exception e) {
            log.error("Error handling email delivery failure: notificationId={}, error={}",
                     notification.getNotificationId(), e.getMessage(), e);

            // Fallback to FAILED status on error handling failure
            try {
                emailRepository.updateStatus(notification.getNotificationId(), NotificationStatus.FAILED.name(), mongoTemplate);
            } catch (Exception fallbackException) {
                log.error("Failed to update status even in fallback: notificationId={}",
                         notification.getNotificationId(), fallbackException);
            }
        }
    }

    /**
     * Handles processing errors with appropriate acknowledgment strategy.
     */
    private void handleProcessingError(String notificationId, String tenantId, Exception error,
                                     Acknowledgment acknowledgment) {
        try {
            if (notificationId != null) {
                var mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
                emailRepository.updateStatus(notificationId, "ERROR", mongoTemplate);
            }

            // Acknowledge to prevent infinite reprocessing of corrupted messages
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error in error handling for notificationId={}: {}",
                     notificationId, e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Checks if a notification has already been successfully processed.
     * Prevents duplicate processing of messages that may be redelivered by Kafka.
     *
     * @param notification The notification to check
     * @return true if the notification has been successfully processed, false otherwise
     */
    private boolean isSuccessfullyProcessed(NotificationEmail notification) {
        String status = notification.getStatus();
        return NotificationStatus.SENT.name().equals(status) ||
               NotificationStatus.DELIVERED.name().equals(status) ||
               NotificationStatus.ACKNOWLEDGED.name().equals(status) ||
               NotificationStatus.RETRIEVED.name().equals(status);
    }

    /**
     * Checks if this Email consumer should process the given event.
     * Only processes events with EMAIL customer identifier type.
     */
    private boolean shouldProcessEvent(EventMessage eventMessage) {
        return NotificationChannel.EMAIL.name().equalsIgnoreCase(eventMessage.getCustomerIdentifiers().getType());
    }

    /**
     * Validates event configuration and checks if EMAIL channel is enabled.
     * If config is missing, uses application property fallback.
     */
    private boolean validateEventConfigurationAndChannelEnabled(EventMessage eventMessage,
                                                               MongoTemplate mongoTemplate) {
        try {
            Optional<EventConfiguration> configOpt = eventConfigurationRepository.findByBusinessIdAndEventType(
                eventMessage.getBusinessId(), eventMessage.getTenantId(), eventMessage.getEventType(), mongoTemplate);

            if (configOpt.isEmpty()) {
                log.warn("Event configuration not found for business: {} and event type: {}, using fallback: {}",
                        eventMessage.getBusinessId(), eventMessage.getEventType(), sendOnMissingConfig);
                return sendOnMissingConfig;
            }

            EventConfiguration eventConfig = configOpt.get();

            boolean emailEnabled = sendOnMissingConfig;

            if ("DATA_PROTECTION_OFFICER".equalsIgnoreCase(eventMessage.getRecipientType())) {
                emailEnabled = eventConfig.getNotifications() != null &&
                        eventConfig.getNotifications().getDataProtectionOfficer() != null &&
                        Boolean.TRUE.equals(eventConfig.getNotifications().getDataProtectionOfficer().getEnabled()) &&
                        eventConfig.getNotifications().getDataProtectionOfficer().getChannels() != null &&
                        eventConfig.getNotifications().getDataProtectionOfficer().getChannels().contains(NotificationChannel.EMAIL.name());
            } else {
                emailEnabled = eventConfig.getNotifications() != null &&
                        eventConfig.getNotifications().getDataPrincipal() != null &&
                        Boolean.TRUE.equals(eventConfig.getNotifications().getDataPrincipal().getEnabled()) &&
                        eventConfig.getNotifications().getDataPrincipal().getChannels() != null &&
                        eventConfig.getNotifications().getDataPrincipal().getChannels().contains(NotificationChannel.EMAIL.name());
            }

            log.debug("Email channel enabled: {} for eventType: {}, business: {}",
                     emailEnabled, eventMessage.getEventType(), eventMessage.getBusinessId());
            return emailEnabled;

        } catch (Exception e) {
            log.error("Error validating event configuration for Email: {}", e.getMessage());
            return sendOnMissingConfig; // Fallback on error
        }
    }

    /**
     * Creates Email notification record from event message.
     */
    private NotificationEmail createEmailNotificationRecord(EventMessage eventMessage, String notificationId,
                                                           MongoTemplate mongoTemplate) {
        // Extract recipientType with default fallback (outside try block for catch block access)
        String recipientType = (eventMessage.getRecipientType() != null && !eventMessage.getRecipientType().isEmpty())
            ? eventMessage.getRecipientType() : "DATA_PRINCIPAL";

        try {

            // Validate template exists and get the actual templateId
            String validatedTemplateId = templateService.validateTemplateExists(
                eventMessage.getEventType(), NotificationChannel.EMAIL.name(), eventMessage.getTenantId(),
                eventMessage.getBusinessId(), eventMessage.getLanguage(), recipientType);

            // Get Email template configuration for email settings (metadata only)
            EmailTemplateConfig emailConfig = getEmailTemplateConfig(validatedTemplateId, eventMessage.getTenantId(), mongoTemplate);

            NotificationEmail emailNotification = NotificationEmail.builder()
                    .notificationId(notificationId)
                    .eventId(eventMessage.getEventId())
                    .eventType(eventMessage.getEventType())
                    .correlationId(eventMessage.getCorrelationId())
                    .businessId(eventMessage.getBusinessId())
                    .templateId(validatedTemplateId)
                    .to(List.of(eventMessage.getCustomerIdentifiers().getValue()))
                    .recipientType(recipientType)
                    .recipientId(eventMessage.getCustomerIdentifiers().getValue())
                    .templateArgs(convertEventPayloadToStringMap(eventMessage.getEventPayload()))
                    .emailType(emailConfig.getEmailType())
                    .fromName(emailConfig.getFromName())
                    .status(NotificationStatus.PENDING.name())
                    .priority(EventPriority.MEDIUM.name())
                    .attemptCount(0)
                    .maxAttempts(retryPolicyService.getMaxRetryAttempts(NotificationChannel.EMAIL.name()))
                    .isAsync(true)
                    .build();

            emailRepository.save(emailNotification, mongoTemplate);
            log.info("Created Email notification record: notificationId={}, eventId={}",
                    notificationId, eventMessage.getEventId());
            return emailNotification;

        } catch (Exception e) {
            log.error("Failed to create Email notification record: {}", e.getMessage());

            // Mark as failed in database for audit
            try {
                NotificationEmail failedNotification = NotificationEmail.builder()
                        .notificationId(notificationId)
                        .eventId(eventMessage.getEventId())
                        .eventType(eventMessage.getEventType())
                        .correlationId(eventMessage.getCorrelationId())
                        .businessId(eventMessage.getBusinessId())
                        .to(List.of(eventMessage.getCustomerIdentifiers().getValue()))
                        .recipientType(recipientType)
                        .recipientId(eventMessage.getCustomerIdentifiers().getValue())
                        .status(NotificationStatus.FAILED.name())
                        .isAsync(true)
                        .build();
                emailRepository.save(failedNotification, mongoTemplate);
            } catch (Exception saveException) {
                log.error("Failed to save failed Email notification record: {}", saveException.getMessage());
            }

            return null;
        }
    }

    /**
     * Generates a unique notification ID for Email using SecureRandom for cryptographic security.
     */
    private String generateNotificationId(String type) {
        return "NOTIF_" + type + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "_" + String.format("%05d", secureRandom.nextInt(100000));
    }


    /**
     * Resolves Email template (subject and body) using Master List Resolution System.
     */
    private Map<String, String> resolveEmailTemplateWithMasterList(String templateId, EventMessage eventMessage,
                                                                  MongoTemplate mongoTemplate) {
        Map<String, String> result = new HashMap<>();
        result.put("subject", "");
        result.put("body", "");

        try {
            // Validate template exists
            String recipientType = (eventMessage.getRecipientType() != null && !eventMessage.getRecipientType().isEmpty())
                ? eventMessage.getRecipientType() : "DATA_PRINCIPAL";
            String validatedTemplateId = templateService.validateTemplateExists(
                eventMessage.getEventType(), NotificationChannel.EMAIL.name(), eventMessage.getTenantId(),
                eventMessage.getBusinessId(), eventMessage.getLanguage(), recipientType);

            if (validatedTemplateId == null) {
                log.error("Template validation failed for templateId: {}", templateId);
                return result;
            }

            // Get the template from database
            var query = new Query()
                .addCriteria(Criteria.where("templateId").is(validatedTemplateId));

            var template = mongoTemplate.findOne(query,
                NotificationTemplate.class);

            if (template == null || template.getEmailConfig() == null) {
                log.error("Email template not found or no Email config: {}", validatedTemplateId);
                return result;
            }

            String emailSubject = template.getEmailConfig().getTemplateSubject();
            String emailBody = template.getEmailConfig().getTemplateBody();

            // Get template's arguments map
            Map<String, String> subjectArgs = template.getEmailConfig().getArgumentsSubjectMap();
            Map<String, String> bodyArgs = template.getEmailConfig().getArgumentsBodyMap();
            Map<String, String> argumentsMap = new HashMap<>();
            if (subjectArgs != null) argumentsMap.putAll(subjectArgs);
            if (bodyArgs != null) argumentsMap.putAll(bodyArgs);

            // Prepare template texts for resolution
            Map<String, String> templateTexts = new HashMap<>();
            if (emailSubject != null) {
                templateTexts.put("emailSubject", emailSubject);
            }
            if (emailBody != null) {
                templateTexts.put("emailContent", emailBody);
            }

            // Use Master List Service to resolve and replace placeholders
            Map<String, String> resolved = masterListService.resolveAndReplaceTemplates(
                argumentsMap, templateTexts, convertEventMessageToTriggerRequest(eventMessage),
                eventMessage.getTenantId(), eventMessage.getBusinessId());

            result.put("subject", resolved.getOrDefault("emailSubject", emailSubject != null ? emailSubject : ""));
            result.put("body", resolved.getOrDefault("emailContent", emailBody != null ? emailBody : ""));

            return result;

        } catch (Exception e) {
            log.error("Failed to resolve Email template {}: {}", templateId, e.getMessage());
            return result;
        }
    }

    /**
     * Resolves email template arguments separately for subject and body using Master List Resolution System.
     */
    private Map<String, Map<String, String>> resolveEmailArgumentsWithMasterList(String templateId, EventMessage eventMessage) {
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("subject", new HashMap<>());
        result.put("body", new HashMap<>());

        // Set EventContext for OTP persistence
        try {
            EventContext.setContext(EventContext.builder()
                .eventId(eventMessage.getEventId())
                .txnId(eventMessage.getTransactionId())
                .correlationId(eventMessage.getCorrelationId())
                .tenantId(eventMessage.getTenantId())
                .businessId(eventMessage.getBusinessId())
                .eventType(eventMessage.getEventType())
                .recipientValue(eventMessage.getCustomerIdentifiers() != null ?
                               eventMessage.getCustomerIdentifiers().getValue() : null)
                .recipientType(eventMessage.getCustomerIdentifiers() != null ?
                              eventMessage.getCustomerIdentifiers().getType() : null)
                .build());

            try {
                MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(eventMessage.getTenantId());
                var query = new Query()
                    .addCriteria(Criteria.where("templateId").is(templateId));

                var template = mongoTemplate.findOne(query,
                    NotificationTemplate.class);

                if (template == null || template.getEmailConfig() == null) {
                    return result;
                }

                // Create headers map from EventMessage fields for header.* path resolution
                Map<String, String> headers = createHeadersMap(eventMessage);

                // Resolve subject arguments
                Map<String, String> argumentsSubjectMap = template.getEmailConfig().getArgumentsSubjectMap();
                if (argumentsSubjectMap != null && !argumentsSubjectMap.isEmpty()) {
                    Map<String, String> resolvedSubjectValues = masterListService.resolveArguments(
                        argumentsSubjectMap, convertEventMessageToTriggerRequest(eventMessage),
                        eventMessage.getTenantId(), eventMessage.getBusinessId(), headers);
                    result.put("subject", resolvedSubjectValues);
                }

                // Resolve body arguments
                Map<String, String> argumentsBodyMap = template.getEmailConfig().getArgumentsBodyMap();
                if (argumentsBodyMap != null && !argumentsBodyMap.isEmpty()) {
                    Map<String, String> resolvedBodyValues = masterListService.resolveArguments(
                        argumentsBodyMap, convertEventMessageToTriggerRequest(eventMessage),
                        eventMessage.getTenantId(), eventMessage.getBusinessId(), headers);
                    result.put("body", resolvedBodyValues);
                }

                return result;

            } catch (Exception e) {
                log.error("Failed to resolve email arguments for template {}: {}", templateId, e.getMessage());
                return result;
            }
        } finally {
            // CRITICAL: Clear context to prevent memory leaks
            EventContext.clearContext();
        }
    }

    /**
     * Creates a headers map from EventMessage fields for use with PayloadResolver.
     * Maps EventMessage fields to standard HTTP header names.
     */
    private Map<String, String> createHeadersMap(EventMessage eventMessage) {
        Map<String, String> headers = new HashMap<>();

        if (eventMessage.getTenantId() != null) {
            headers.put("X-Tenant-Id", eventMessage.getTenantId());
        }
        if (eventMessage.getBusinessId() != null) {
            headers.put("X-Business-Id", eventMessage.getBusinessId());
        }
        if (eventMessage.getTransactionId() != null) {
            headers.put("X-Transaction-Id", eventMessage.getTransactionId());
        }
        if (eventMessage.getCorrelationId() != null) {
            headers.put("X-Correlation-Id", eventMessage.getCorrelationId());
        }
        if (eventMessage.getEventId() != null) {
            headers.put("X-Event-Id", eventMessage.getEventId());
        }
        if (eventMessage.getMessageId() != null) {
            headers.put("X-Message-Id", eventMessage.getMessageId());
        }
        if (eventMessage.getSourceIp() != null) {
            headers.put("X-Source-IP", eventMessage.getSourceIp());
        }

        log.debug("Created headers map with {} entries for PayloadResolver", headers.size());
        return headers;
    }

    /**
     * Converts EventMessage to TriggerEventRequestDto for Master List Service compatibility.
     */
    private com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto convertEventMessageToTriggerRequest(
            EventMessage eventMessage) {

        var request = new TriggerEventRequestDto();
        request.setEventType(eventMessage.getEventType());
        request.setResource(eventMessage.getResource());
        request.setSource(eventMessage.getSource());
        request.setLanguage(eventMessage.getLanguage());
        request.setDataProcessorIds(eventMessage.getDataProcessorIds());
        request.setEventPayload(eventMessage.getEventPayload());

        var customerIds = new TriggerEventRequestDto.CustomerIdentifiersDto();
        customerIds.setType(eventMessage.getCustomerIdentifiers().getType());
        customerIds.setValue(eventMessage.getCustomerIdentifiers().getValue());
        request.setCustomerIdentifiers(customerIds);

        return request;
    }

    /**
     * Retrieves Email template configuration for email settings.
     * The templateId passed here should be the validated templateId returned by templateService.validateTemplateExists()
     * This method gets the template once and extracts the necessary config fields for notification creation.
     */
    private EmailTemplateConfig getEmailTemplateConfig(String validatedTemplateId, String tenantId,
                                                      MongoTemplate mongoTemplate) {
        try {
            var query = new Query()
                    .addCriteria(Criteria.where("templateId").is(validatedTemplateId));

            NotificationTemplate template = mongoTemplate.findOne(query, NotificationTemplate.class);

            if (template != null && template.getEmailConfig() != null) {
                var emailConfig = template.getEmailConfig();

                // Validate email template configuration is present
                if (emailConfig.getTemplateSubject() == null || emailConfig.getTemplateSubject().trim().isEmpty() ||
                    emailConfig.getTemplateBody() == null || emailConfig.getTemplateBody().trim().isEmpty()) {
                    log.error("Invalid email template configuration for templateId: {}. Subject: {}, Body present: {}",
                            validatedTemplateId, emailConfig.getTemplateSubject(),
                            emailConfig.getTemplateBody() != null && !emailConfig.getTemplateBody().trim().isEmpty());
                    throw new BusinessException(ErrorCode.INVALID_EMAIL_TEMPLATE.getCode(),
                        ErrorMessage.INVALID_EMAIL_TEMPLATE_MSG.format(validatedTemplateId));
                }

                return EmailTemplateConfig.builder()
                        .subject(emailConfig.getTemplateSubject())
                        .body(emailConfig.getTemplateBody())
                        .emailType(emailConfig.getEmailType())
                        .fromName(emailConfig.getTemplateFromName())
                        .build();
            } else {
                log.error("Email template configuration not found for templateId: {}. Cannot send email without valid template configuration.", validatedTemplateId);
                throw new BusinessException(ErrorCode.EMAIL_TEMPLATE_NOT_FOUND.getCode(),
                    ErrorMessage.EMAIL_TEMPLATE_NOT_FOUND_MSG.format(validatedTemplateId));
            }
        } catch (BusinessException e) {
            // Re-throw business exceptions to maintain error flow
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Email template configuration for templateId: {}. Cannot send email without valid configuration.", validatedTemplateId);
            throw new BusinessException(ErrorCode.EMAIL_CONFIG_FETCH_ERROR.getCode(),
                ErrorMessage.EMAIL_CONFIG_FETCH_ERROR_MSG.format(validatedTemplateId), e);
        }
    }

    /**
     * Converts event payload to string map for template arguments.
     */
    private Map<String, Object> convertEventPayloadToStringMap(Map<String, Object> eventPayload) {
        Map<String, Object> stringMap = new HashMap<>();
        if (eventPayload != null) {
            eventPayload.forEach((key, value) -> {
                if (value instanceof Map) {
                    stringMap.put(key, value);
                } else {
                    stringMap.put(key, value != null ? value.toString() : "");
                }
            });
        }
        return stringMap;
    }

    /**
     * Email template configuration holder.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class EmailTemplateConfig {
        private java.util.List<String> cc;
        private String subject;
        private String body;
        private String emailType;
        private String fromName;
    }

}