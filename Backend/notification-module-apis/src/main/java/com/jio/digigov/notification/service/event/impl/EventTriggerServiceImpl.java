package com.jio.digigov.notification.service.event.impl;

import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.dto.response.event.TriggerEventResponseDto;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.entity.event.NotificationEvent;
import com.jio.digigov.notification.enums.EventStatus;
import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.mapper.NotificationEventMapper;
import com.jio.digigov.notification.repository.event.NotificationEventRepository;
import com.jio.digigov.notification.service.event.EventConfigurationService;
import com.jio.digigov.notification.service.event.EventTriggerService;
import com.jio.digigov.notification.service.kafka.AsyncNotificationProducerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import com.jio.digigov.notification.util.MongoTemplateProvider;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventTriggerServiceImpl implements EventTriggerService {

    private final EventConfigurationService eventConfigurationService;
    private final NotificationEventRepository notificationEventRepository;
    private final NotificationEventMapper notificationEventMapper;
    private final MongoTemplateProvider mongoTemplateProvider;
    private final AsyncNotificationProducerService asyncNotificationProducerService;

    @Override
    public TriggerEventResponseDto triggerEvent(TriggerEventRequestDto request,
                                           String tenantId,
                                           String businessId,
                                           String transactionId,
                                           HttpServletRequest httpRequest) {
        log.info("Triggering event: {} for tenant: {}, business: {}, transactionId: {}",
                request.getEventType(), tenantId, businessId, transactionId);

        // Get tenant-specific mongoTemplate
        MongoTemplate tenantTemplate = mongoTemplateProvider.getTemplate(tenantId);

        try {
            // Validate event configuration first
            EventConfiguration eventConfig = eventConfigurationService.validateEventConfiguration(
                businessId, request.getEventType(), tenantId);

            // Use async processing with Master List Resolution
            var asyncResponseFuture = asyncNotificationProducerService.processEventAsync(
                request, tenantId, businessId, transactionId, httpRequest);

            var asyncResponse = asyncResponseFuture.get();

            log.info("Successfully triggered async event: {} with ID: {} for business: {}",
                    request.getEventType(), asyncResponse.getEventId(), businessId);

            return TriggerEventResponseDto.builder()
                    .eventId(asyncResponse.getEventId())
                    .eventType(request.getEventType())
                    .transactionId(asyncResponse.getTransactionId())
                    .status("PROCESSING")
                    .message("Event accepted for async processing")
                    .build();

        } catch (Exception e) {
            log.error("Failed to trigger async event: {} for business: {}", request.getEventType(), businessId);

            // Create a minimal failed event record for tracking
            NotificationEvent failedEvent = createFailedNotificationEvent(request, businessId, e.getMessage());
            NotificationEvent saved = tenantTemplate.save(failedEvent);

            return TriggerEventResponseDto.builder()
                    .eventId(saved.getEventId())
                    .eventType(saved.getEventType())
                    .status(NotificationStatus.FAILED.name())
                    .message("Event processing failed: " + e.getMessage())
                    .build();
        }
    }

    // All notification processing is now handled by AsyncNotificationProducerService
    // with Master List Resolution integrated

    private NotificationEvent createNotificationEvent(TriggerEventRequestDto request, 
                                                    String businessId,
                                                    NotificationEvent.NotificationsSummary summary) {
        
        NotificationEvent notificationEvent = notificationEventMapper.toEntity(request, businessId, request.getLanguage() != null ? request.getLanguage() : "english");
        notificationEvent.setBusinessId(businessId);
        notificationEvent.setStatus(summary.getFailedCount() == 0 ?
                EventStatus.COMPLETED : EventStatus.PARTIALLY_FAILED);
        notificationEvent.setNotificationsSummary(summary);

        return notificationEvent;
    }

    private NotificationEvent createFailedNotificationEvent(TriggerEventRequestDto request, 
                                                          String businessId, 
                                                          String errorMessage) {
        
        NotificationEvent notificationEvent = notificationEventMapper.toEntity(request, businessId, request.getLanguage() != null ? request.getLanguage() : "english");
        notificationEvent.setBusinessId(businessId);
        notificationEvent.setStatus(EventStatus.FAILED);
        // Note: Error stored in event payload for tracking
        
        NotificationEvent.NotificationsSummary summary = new NotificationEvent.NotificationsSummary();
        summary.setTotalCount(0);
        summary.setCompletedCount(0);
        summary.setFailedCount(0);
        notificationEvent.setNotificationsSummary(summary);
        
        return notificationEvent;
    }
}