package com.jio.digigov.notification.mapper;

import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.entity.event.NotificationEvent;
import com.jio.digigov.notification.enums.EventStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class NotificationEventMapper {

    public NotificationEvent toEntity(TriggerEventRequestDto request, String businessId, String language) {
        return NotificationEvent.builder()
                .eventId(generateEventId())
                .businessId(businessId)
                .eventType(request.getEventType())
                .resource(request.getResource())
                .source(request.getSource())
                .language(language)
                .customerIdentifiers(mapCustomerIdentifiers(request.getCustomerIdentifiers()))
                .dataProcessorIds(request.getDataProcessorIds())
                .eventPayload(request.getEventPayload())
                .notificationsSummary(NotificationEvent.NotificationsSummary.builder()
                        .totalCount(0)
                        .smsCount(0)
                        .emailCount(0)
                        .callbackCount(0)
                        .completedCount(0)
                        .failedCount(0)
                        .build())
                .status(EventStatus.PENDING)
                .build();
    }

    private NotificationEvent.CustomerIdentifiers mapCustomerIdentifiers(
            TriggerEventRequestDto.CustomerIdentifiersDto dto) {
        if (dto == null) {
            return null;
        }
        
        return NotificationEvent.CustomerIdentifiers.builder()
                .type(dto.getType())
                .value(dto.getValue())
                .build();
    }

    private String generateEventId() {
        return "EVT_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}