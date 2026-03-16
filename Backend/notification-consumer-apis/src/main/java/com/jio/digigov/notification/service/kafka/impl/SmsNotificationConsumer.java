package com.jio.digigov.notification.service.kafka.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.kafka.EventMessage;
import com.jio.digigov.notification.dto.kafka.SmsPayload;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.entity.notification.NotificationSms;
import com.jio.digigov.notification.enums.CredentialType;
import com.jio.digigov.notification.enums.ErrorCode;
import com.jio.digigov.notification.enums.ErrorMessage;
import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.exception.BusinessException;
import com.jio.digigov.notification.repository.event.EventConfigurationRepository;
import com.jio.digigov.notification.repository.notification.NotificationSmsRepository;
import com.jio.digigov.notification.service.DigiGovClientService;
import com.jio.digigov.notification.service.TokenService;
import com.jio.digigov.notification.service.audit.AuditEventService;
import com.jio.digigov.notification.service.kafka.RetryPolicyService;
import com.jio.digigov.notification.service.template.TemplateService;
import com.jio.digigov.notification.service.masterlist.MasterListService;
import com.jio.digigov.notification.util.JwtTokenUtil;
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
 * Kafka consumer for processing SMS notification messages.
 *
 * This service listens to the SMS notification topic and processes messages by:
 * 1. Resolving SMS templates with dynamic arguments
 * 2. Integrating with DigiGov SMS API for delivery
 * 3. Updating notification status in tenant-specific databases
 * 4. Implementing retry logic with exponential backoff
 * 5. Handling TRAI DLT compliance requirements
 *
 * Performance Features:
 * - Parallel processing with configurable concurrency
 * - Token caching for DigiGov API authentication
 * - Manual acknowledgment for reliable message processing
 * - Circuit breaker pattern for external API resilience
 * - Batch processing for high-throughput scenarios
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsNotificationConsumer {

    private final NotificationSmsRepository smsRepository;
    private final EventConfigurationRepository eventConfigurationRepository;
    private final TemplateService templateService;
    private final RetryPolicyService retryPolicyService;
    private final MongoTemplateProvider mongoTemplateProvider;
    private final DigiGovClientService digiGovClientService;
    private final TokenService tokenService;
    private final MasterListService masterListService;
    private final ObjectMapper objectMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuditEventService auditEventService;

    @Value("${notification.sms.send-on-missing-config:true}")
    private boolean sendOnMissingConfig;

    /**
     * SecureRandom instance for cryptographically secure notification ID generation
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Main entry point for SMS event processing.
     * Processes event messages with validation, notification creation, and delivery.
     */
    @KafkaListener(
        topics = "${kafka.topics.sms}",
        groupId = "${kafka.consumer.group-id}",
        containerFactory = "smsKafkaListenerContainerFactory"
    )
    public void processSmsNotification(
            @Payload EventMessage eventMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Processing SMS event: eventId={}, messageId={}, topic={}, partition={}, offset={}",
                eventMessage.getEventId(), eventMessage.getMessageId(), topic, partition, offset);

        String notificationId = null;
        try {
            // Check if this consumer should process this event
            if (!shouldProcessEvent(eventMessage)) {
                log.debug("SMS consumer skipping event - customer identifier type: {}",
                         eventMessage.getCustomerIdentifiers().getType());
                acknowledgment.acknowledge();
                return;
            }

            // Get tenant-specific MongoTemplate
            var mongoTemplate = mongoTemplateProvider.getTemplate(eventMessage.getTenantId());

            // Validate event configuration
            boolean shouldSend = validateEventConfigurationAndChannelEnabled(eventMessage, mongoTemplate);
            if (!shouldSend) {
                log.info("SMS not enabled for event or missing config - eventId: {}", eventMessage.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // Generate notification ID and create notification record
            notificationId = generateNotificationId(NotificationChannel.SMS.name());
            NotificationSms smsNotification = createSmsNotificationRecord(eventMessage, notificationId, mongoTemplate);
            if (smsNotification == null) {
                log.error("Failed to create SMS notification record - eventId: {}", eventMessage.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // Update status to PROCESSING
            smsRepository.updateStatus(notificationId, NotificationStatus.PROCESSING.name(), mongoTemplate);

            // Process the SMS notification
            processSms(smsNotification, eventMessage, mongoTemplate);

            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.info("Successfully processed SMS event: eventId={}, notificationId={}",
                    eventMessage.getEventId(), notificationId);

        } catch (Exception e) {
            log.error("Error processing SMS event: eventId={}, notificationId={}, error={}",
                     eventMessage.getEventId(), notificationId, e.getMessage(), e);

            handleProcessingError(notificationId, eventMessage.getTenantId(), e, acknowledgment);
        }
    }

    /**
     * Core SMS processing logic with template resolution and DigiGov API integration.
     */
    private void processSms(NotificationSms notification, EventMessage eventMessage,
                           MongoTemplate mongoTemplate) {

        try {
            // Get configuration and authentication token
            NotificationConfig config = getConfiguration(eventMessage.getTenantId(), eventMessage.getBusinessId(), mongoTemplate);
            String token = tokenService.generateTokenWithConfig(config, CredentialType.CLIENT).getAccessToken();

            // Get resolved arguments from Master List Resolution
            Map<String, Object> resolvedArgs = resolveArgumentsWithMasterList(
                notification.getTemplateId(), NotificationChannel.SMS.name(), eventMessage);

            // Prepare request for DigiGov service
            Map<String, Object> smsJson = new HashMap<>();
            smsJson.put("templateId", notification.getTemplateId());
            smsJson.put("mobileNumber", notification.getMobile());
            smsJson.put("messagedetails", "SMS notification");
            smsJson.put("args", resolvedArgs);

            // Convert to JSON string
            String smsJsonString = objectMapper.writeValueAsString(smsJson);

            log.info("SMS JSON being sent to DigiGov: {}", smsJsonString);

            // Prepare form fields
            org.springframework.util.MultiValueMap<String, Object> formData = new org.springframework.util.LinkedMultiValueMap<>();
            formData.add("smsReq", smsJsonString);

            // Create headers map for DigiGov service
            Map<String, String> headers = new HashMap<>();
            headers.put("txn", eventMessage.getTransactionId());

            // Send notification through DigiGov unified endpoint
            Map<String, Object> response = digiGovClientService.sendNotification(formData, config, token, headers);

            // Handle the response (similar to SendNotificationServiceImpl)
            String responseStatus = (String) response.getOrDefault("smsStatus", "Failed");
            String txnId = (String) response.get("txn");

            boolean isSuccess = "Request Accepted".equals(responseStatus);

            if (isSuccess) {
                smsRepository.updateDeliveryInfo(
                        notification.getNotificationId(),
                        NotificationStatus.SENT.name(),
                        LocalDateTime.now(),
                        response,
                        mongoTemplate
                );

                // Record success metrics
                retryPolicyService.recordRetryMetrics(NotificationChannel.SMS.name(), notification.getAttemptCount(), true, null);

                // Audit successful delivery
                auditEventService.auditNotificationDelivery(
                        notification.getNotificationId(),
                        "DELIVERY_SUCCESS",
                        "SMS_CONSUMER",
                        eventMessage.getTenantId(),
                        eventMessage.getBusinessId(),
                        eventMessage.getTransactionId(),
                        eventMessage.getSourceIp(),
                        eventMessage.getEventId()
                );

                log.info("SMS sent successfully: notificationId={}, txnId={}, status={}, attempts={}",
                        notification.getNotificationId(), txnId, responseStatus, notification.getAttemptCount());
            } else {
                // Audit failed delivery
                auditEventService.auditNotificationDelivery(
                        notification.getNotificationId(),
                        "DELIVERY_FAILED",
                        "SMS_CONSUMER",
                        eventMessage.getTenantId(),
                        eventMessage.getBusinessId(),
                        eventMessage.getTransactionId(),
                        eventMessage.getSourceIp(),
                        eventMessage.getEventId()
                );

                handleDeliveryFailure(notification, responseStatus, mongoTemplate);
            }

        } catch (Exception e) {
            log.error("Error processing SMS for notificationId={}: {}",
                     notification.getNotificationId(), e.getMessage(), e);

            // Audit failed delivery
            auditEventService.auditNotificationDelivery(
                    notification.getNotificationId(),
                    "DELIVERY_FAILED",
                    "SMS_CONSUMER",
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
     * Get configuration for the tenant and business (fixed to use businessId from Kafka message)
     */
    /**
     * Get configuration with 3-level fallback logic using MongoTemplate.
     * Step 1: Try businessId
     * Step 2: Try tenantId as businessId
     * Step 3: Try scopeLevel=TENANT
     */
    private NotificationConfig getConfiguration(String tenantId, String businessId, MongoTemplate mongoTemplate) {
        // Set tenant context for configuration lookup
        TenantContextHolder.setTenantId(tenantId);

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

        // Step 2: Try to find by tenantId as businessId
        log.debug("Configuration not found for businessId: {}, trying tenantId as businessId fallback (Step 2)", businessId);
        Query step2Query = new Query(Criteria.where("businessId").is(tenantId));
        config = mongoTemplate.findOne(
                step2Query,
                NotificationConfig.class,
                "notification_configurations"
        );

        if (config != null) {
            log.info("Configuration found using tenantId as businessId fallback (Step 2: tenantId match): tenantId={}", tenantId);
            return config;
        }

        // Step 3: Try to find by scopeLevel = TENANT
        log.debug("Configuration not found for tenantId as businessId, trying scopeLevel=TENANT fallback (Step 3)");
        Query step3Query = new Query(Criteria.where("scopeLevel").is("TENANT"));
        config = mongoTemplate.findOne(
                step3Query,
                NotificationConfig.class,
                "notification_configurations"
        );

        if (config != null) {
            log.info("Configuration found using TENANT-level fallback (Step 3: scopeLevel=TENANT match): tenantId={}", tenantId);
            return config;
        }

        // If all three steps fail, throw exception
        log.error("Configuration not found after all 3 fallback steps for businessId: {}, tenantId: {}", businessId, tenantId);
        throw new BusinessException("CONFIG_NOT_FOUND",
                "NotificationConfig not found for businessId: " + businessId + " after all fallback attempts");
    }

    /**
     * Handles SMS delivery failures with retry logic.
     */
    private void handleDeliveryFailure(NotificationSms notification, String errorMessage,
                                     MongoTemplate mongoTemplate) {
        try {
            int currentAttempt = notification.getAttemptCount();
            String channel = NotificationChannel.SMS.name();

            // Check if error is retryable
            if (!retryPolicyService.isRetryableError(errorMessage, channel)) {
                log.warn("SMS delivery failed with non-retryable error: notificationId={}, error={}",
                        notification.getNotificationId(), errorMessage);

                // Mark as permanently failed for non-retryable errors
                smsRepository.updateStatus(notification.getNotificationId(), NotificationStatus.FAILED.name(), mongoTemplate);
                retryPolicyService.recordRetryMetrics(channel, currentAttempt, false, "NON_RETRYABLE");
                return;
            }

            // Check if we should retry based on attempt count
            if (retryPolicyService.shouldRetry(currentAttempt, channel)) {
                // Calculate next retry time with exponential backoff
                LocalDateTime nextRetryAt = retryPolicyService.calculateNextRetry(currentAttempt, channel);

                // Update retry information
                smsRepository.updateRetryInfo(
                        notification.getNotificationId(),
                        currentAttempt + 1,
                        nextRetryAt,
                        errorMessage,
                        mongoTemplate
                );

                // Update status to RETRY_SCHEDULED
                smsRepository.updateStatus(notification.getNotificationId(), "RETRY_SCHEDULED", mongoTemplate);

                retryPolicyService.recordRetryMetrics(channel, currentAttempt, false, "RETRYABLE");

                log.warn("SMS delivery failed, scheduling retry: notificationId={}, attempt={}, nextRetry={}, error={}",
                        notification.getNotificationId(), currentAttempt + 1, nextRetryAt, errorMessage);
            } else {
                // Mark as permanently failed after max attempts
                smsRepository.updateStatus(notification.getNotificationId(), NotificationStatus.FAILED.name(), mongoTemplate);

                retryPolicyService.recordRetryMetrics(channel, currentAttempt, false, "MAX_ATTEMPTS_EXCEEDED");

                log.error("SMS delivery permanently failed after {} attempts: notificationId={}, finalError={}",
                         retryPolicyService.getMaxRetryAttempts(channel), notification.getNotificationId(), errorMessage);
            }

        } catch (Exception e) {
            log.error("Error handling SMS delivery failure: notificationId={}, error={}",
                     notification.getNotificationId(), e.getMessage(), e);

            // Fallback to FAILED status on error handling failure
            try {
                smsRepository.updateStatus(notification.getNotificationId(), NotificationStatus.FAILED.name(), mongoTemplate);
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
                smsRepository.updateStatus(notificationId, "ERROR", mongoTemplate);
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
     * Creates a default SMS message when template resolution fails.
     *
     * @param notification The SMS notification record
     * @param smsPayload The SMS payload from Kafka message
     * @return Default SMS message text
     */
    private String createDefaultSmsMessage(NotificationSms notification, SmsPayload smsPayload) {
        String baseMessage = "DPDP Notification: Your data processing event has been completed";

        // Try to include some basic template arguments if available
        if (smsPayload.getTemplateArgs() != null && !smsPayload.getTemplateArgs().isEmpty()) {
            StringBuilder messageBuilder = new StringBuilder(baseMessage);

            // Ensure message doesn't exceed SMS length
            String finalMessage = messageBuilder.toString();
            if (finalMessage.length() > 160) {
                return baseMessage; // Return basic message if enhanced version is too long
            }
            return finalMessage;
        }

        return baseMessage;
    }

    /**
     * Checks if a notification has already been successfully processed.
     * Prevents duplicate processing of messages that may be redelivered by Kafka.
     *
     * @param notification The notification to check
     * @return true if the notification has been successfully processed, false otherwise
     */
    private boolean isSuccessfullyProcessed(NotificationSms notification) {
        String status = notification.getStatus();
        return NotificationStatus.SENT.name().equals(status) ||
               NotificationStatus.DELIVERED.name().equals(status) ||
               "ACKNOWLEDGED".equals(status) ||
               "RETRIEVED".equals(status);
    }

    /**
     * Checks if this SMS consumer should process the given event.
     * Only processes events with MOBILE customer identifier type.
     */
    private boolean shouldProcessEvent(EventMessage eventMessage) {
        return "MOBILE".equalsIgnoreCase(eventMessage.getCustomerIdentifiers().getType());
    }

    /**
     * Validates event configuration and checks if SMS channel is enabled.
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
            boolean smsEnabled = eventConfig.getNotifications() != null &&
                                eventConfig.getNotifications().getDataPrincipal() != null &&
                                Boolean.TRUE.equals(eventConfig.getNotifications().getDataPrincipal().getEnabled()) &&
                                eventConfig.getNotifications().getDataPrincipal().getChannels() != null &&
                                eventConfig.getNotifications().getDataPrincipal().getChannels().contains(NotificationChannel.SMS.name());

            log.debug("SMS channel enabled: {} for eventType: {}, business: {}",
                     smsEnabled, eventMessage.getEventType(), eventMessage.getBusinessId());
            return smsEnabled;

        } catch (Exception e) {
            log.error("Error validating event configuration for SMS: {}", e.getMessage());
            return sendOnMissingConfig; // Fallback on error
        }
    }

    /**
     * Creates SMS notification record from event message.
     */
    private NotificationSms createSmsNotificationRecord(EventMessage eventMessage, String notificationId,
                                                       MongoTemplate mongoTemplate) {
        // Extract recipientType with default fallback (outside try block for catch block access)
        String recipientType = (eventMessage.getRecipientType() != null && !eventMessage.getRecipientType().isEmpty())
            ? eventMessage.getRecipientType() : "DATA_PRINCIPAL";

        try {

            // Validate template exists and get the actual templateId
            String validatedTemplateId = templateService.validateTemplateExists(
                eventMessage.getEventType(), NotificationChannel.SMS.name(), eventMessage.getTenantId(),
                eventMessage.getBusinessId(), eventMessage.getLanguage(), recipientType);

            // Get SMS template configuration for DLT and delivery settings
            SmsTemplateConfig smsConfig = getSmsTemplateConfig(validatedTemplateId, eventMessage.getTenantId(), mongoTemplate);

            NotificationSms smsNotification = NotificationSms.builder()
                    .notificationId(notificationId)
                    .eventId(eventMessage.getEventId())
                    .eventType(eventMessage.getEventType())
                    .correlationId(eventMessage.getCorrelationId())
                    .businessId(eventMessage.getBusinessId())
                    .templateId(validatedTemplateId)
                    .mobile(eventMessage.getCustomerIdentifiers().getValue())
                    .recipientType(recipientType)
                    .recipientId(eventMessage.getCustomerIdentifiers().getValue())
                    .templateArgs(convertEventPayloadToStringMap(eventMessage.getEventPayload()))
                    // Populate from template configuration
                    .dltEntityId(smsConfig.getDltEntityId())
                    .dltTemplateId(smsConfig.getDltTemplateId())
                    .from(smsConfig.getFrom())
                    .oprCountries(smsConfig.getOprCountries())
                    .status(NotificationStatus.PENDING.name())
                    .priority(EventPriority.HIGH.name())
                    .attemptCount(0)
                    .maxAttempts(retryPolicyService.getMaxRetryAttempts(NotificationChannel.SMS.name()))
                    .isAsync(true)
                    .build();

            smsRepository.save(smsNotification, mongoTemplate);
            log.info("Created SMS notification record: notificationId={}, eventId={}",
                    notificationId, eventMessage.getEventId());
            return smsNotification;

        } catch (Exception e) {
            log.error("Failed to create SMS notification record: {}", e.getMessage());

            // Mark as failed in database for audit
            try {
                NotificationSms failedNotification = NotificationSms.builder()
                        .notificationId(notificationId)
                        .eventId(eventMessage.getEventId())
                        .eventType(eventMessage.getEventType())
                        .correlationId(eventMessage.getCorrelationId())
                        .businessId(eventMessage.getBusinessId())
                        .mobile(eventMessage.getCustomerIdentifiers().getValue())
                        .recipientType(recipientType)
                        .recipientId(eventMessage.getCustomerIdentifiers().getValue())
                        .status(NotificationStatus.FAILED.name())
                        .isAsync(true)
                        .build();
                smsRepository.save(failedNotification, mongoTemplate);
            } catch (Exception saveException) {
                log.error("Failed to save failed SMS notification record: {}", saveException.getMessage());
            }

            return null;
        }
    }

    /**
     * Generates a unique notification ID for SMS using SecureRandom for cryptographic security.
     */
    private String generateNotificationId(String type) {
        return "NOTIF_" + type + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "_" + String.format("%05d", secureRandom.nextInt(100000));
    }


    /**
     * Resolves SMS template using Master List Resolution System.
     */
    private String resolveTemplateWithMasterList(String templateId, EventMessage eventMessage,
                                                MongoTemplate mongoTemplate) {
        try {
            // Validate template exists
            String recipientType = (eventMessage.getRecipientType() != null && !eventMessage.getRecipientType().isEmpty())
                ? eventMessage.getRecipientType() : "DATA_PRINCIPAL";
            String validatedTemplateId = templateService.validateTemplateExists(
                eventMessage.getEventType(), NotificationChannel.SMS.name(), eventMessage.getTenantId(),
                eventMessage.getBusinessId(), eventMessage.getLanguage(), recipientType);

            if (validatedTemplateId == null) {
                log.error("Template validation failed for templateId: {}", templateId);
                return null;
            }

            // Get the template from database
            var query = new Query()
                .addCriteria(Criteria.where("templateId").is(validatedTemplateId));

            var template = mongoTemplate.findOne(query,
                NotificationTemplate.class);

            if (template == null || template.getSmsConfig() == null) {
                log.error("SMS template not found or no SMS config: {}", validatedTemplateId);
                return null;
            }

            String templateContent = template.getSmsConfig().getTemplate();
            if (templateContent == null || templateContent.isEmpty()) {
                log.error("SMS template content is empty: {}", validatedTemplateId);
                return null;
            }

            // Get template's arguments map
            Map<String, String> argumentsMap = template.getSmsConfig().getArgumentsMap();
            if (argumentsMap == null) {
                argumentsMap = new HashMap<>();
            }

            // Prepare template texts for resolution
            Map<String, String> templateTexts = new HashMap<>();
            templateTexts.put("smsContent", templateContent);

            // Use Master List Service to resolve and replace placeholders
            Map<String, String> resolved = masterListService.resolveAndReplaceTemplates(
                argumentsMap, templateTexts, convertEventMessageToTriggerRequest(eventMessage),
                eventMessage.getTenantId(), eventMessage.getBusinessId());

            return resolved.getOrDefault("smsContent", templateContent);

        } catch (Exception e) {
            log.error("Failed to resolve SMS template {}: {}", templateId, e.getMessage());
            return null;
        }
    }

    /**
     * Resolves template arguments using Master List Resolution System.
     */
    private Map<String, Object> resolveArgumentsWithMasterList(String templateId, String channel,
                                                              EventMessage eventMessage) {
        try {
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(eventMessage.getTenantId());

            // Check if this is a DPO SMS (recipientType == DATA_PROTECTION_OFFICER)
            boolean isDpoSms = "DATA_PROTECTION_OFFICER".equals(eventMessage.getRecipientType());

            TriggerEventRequestDto requestForResolution = null;

            if (isDpoSms) {
                log.info("Processing DPO SMS notification: eventId={}, messageId={}",
                    eventMessage.getEventId(), eventMessage.getMessageId());
                
                // Decode JWT token to get event payload for template resolution
                if (eventMessage.getJwtToken() != null && !eventMessage.getJwtToken().isEmpty()) {
                    try {
                        Map<String, Object> decodedToken = jwtTokenUtil.decodeToken(eventMessage.getJwtToken());
                        Map<String, Object> eventPayload = jwtTokenUtil.extractEventPayload(decodedToken);
                        
                        log.debug("Decoded JWT token for DPO SMS: eventType={}",
                            jwtTokenUtil.extractField(decodedToken, "eventType"));
                        
                        requestForResolution = convertEventMessageToTriggerRequest(eventMessage);
                        requestForResolution.setEventPayload(eventPayload);
                    } catch (Exception e) {
                        log.error("Failed to decode JWT token for DPO SMS: eventId={}, error={}",
                            eventMessage.getEventId(), e.getMessage(), e);
                        throw new BusinessException(ErrorCode.INTERNAL_PROCESSING_ERROR.getCode(),
                            "Failed to decode DPO SMS JWT token");
                    }
                } else {
                    log.warn("DPO SMS missing JWT token: eventId={}", eventMessage.getEventId());
                    requestForResolution = convertEventMessageToTriggerRequest(eventMessage);
                }
            } else {
                // Regular Data Principal SMS processing
                requestForResolution = convertEventMessageToTriggerRequest(eventMessage);
            }

            var query = new Query()
                .addCriteria(Criteria.where("templateId").is(templateId));

            var template = mongoTemplate.findOne(query,
                NotificationTemplate.class);

            if (template == null || template.getSmsConfig() == null) {
                return new HashMap<>();
            }

            Map<String, String> argumentsMap = template.getSmsConfig().getArgumentsMap();
            if (argumentsMap == null || argumentsMap.isEmpty()) {
                return new HashMap<>();
            }

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

                // Create headers map from EventMessage fields for header.* path resolution
                Map<String, String> headers = createHeadersMap(eventMessage);

                // Use Master List Service to resolve arguments with the appropriate request
                Map<String, String> resolvedValues = masterListService.resolveArguments(
                    argumentsMap, requestForResolution,
                    eventMessage.getTenantId(), eventMessage.getBusinessId(), headers);

                return new HashMap<>(resolvedValues);

            } finally {
                // CRITICAL: Clear context to prevent memory leaks
                EventContext.clearContext();
            }

        } catch (Exception e) {
            log.error("Failed to resolve arguments for template {}: {}", templateId, e.getMessage());
            return new HashMap<>();
        }
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
     * Retrieves SMS template configuration for DLT and delivery settings.
     * The templateId passed here should be the validated templateId returned by templateService.validateTemplateExists()
     */
    private SmsTemplateConfig getSmsTemplateConfig(String validatedTemplateId, String tenantId,
                                                  MongoTemplate mongoTemplate) {
        try {
            var query = new Query()
                    .addCriteria(Criteria.where("templateId").is(validatedTemplateId));

            NotificationTemplate template =
                mongoTemplate.findOne(query, NotificationTemplate.class);

            if (template != null && template.getSmsConfig() != null) {
                var smsConfig = template.getSmsConfig();

                // Validate DLT configuration is present
                if (smsConfig.getDltEntityId() == null || smsConfig.getDltEntityId().trim().isEmpty() ||
                    smsConfig.getDltTemplateId() == null || smsConfig.getDltTemplateId().trim().isEmpty()) {
                    log.error("Invalid DLT configuration for templateId: {}. DLT Entity ID: {}, DLT Template ID: {}",
                            validatedTemplateId, smsConfig.getDltEntityId(), smsConfig.getDltTemplateId());
                    throw new BusinessException(ErrorCode.INVALID_DLT_CONFIG.getCode(),
                        ErrorMessage.INVALID_DLT_CONFIG_MSG.format(validatedTemplateId));
                }

                return SmsTemplateConfig.builder()
                        .dltEntityId(smsConfig.getDltEntityId())
                        .dltTemplateId(smsConfig.getDltTemplateId())
                        .from(smsConfig.getFrom())
                        .oprCountries(smsConfig.getOprCountries())
                        .build();
            } else {
                log.error("SMS template configuration not found for templateId: {}. Cannot send SMS without valid DLT configuration.", validatedTemplateId);
                throw new BusinessException(ErrorCode.SMS_TEMPLATE_NOT_FOUND.getCode(),
                    ErrorMessage.SMS_TEMPLATE_NOT_FOUND_MSG.format(validatedTemplateId));
            }
        } catch (BusinessException e) {
            // Re-throw business exceptions to maintain error flow
            throw e;
        } catch (Exception e) {
            log.error("Error fetching SMS template configuration for templateId: {}. Cannot send SMS without valid configuration.", validatedTemplateId);
            throw new BusinessException(ErrorCode.SMS_CONFIG_FETCH_ERROR.getCode(),
                ErrorMessage.SMS_CONFIG_FETCH_ERROR_MSG.format(validatedTemplateId), e);
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
     * SMS template configuration holder.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class SmsTemplateConfig {
        private String dltEntityId;
        private String dltTemplateId;
        private String from;
        private java.util.List<String> oprCountries;
    }

}