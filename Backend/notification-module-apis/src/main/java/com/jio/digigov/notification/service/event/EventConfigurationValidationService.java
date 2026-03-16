package com.jio.digigov.notification.service.event;

import com.jio.digigov.notification.dto.request.event.CreateEventConfigurationRequestDto;
import com.jio.digigov.notification.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for sanitizing event configuration requests according to business rules.
 *
 * Business Rules:
 * 1. Channels (SMS, EMAIL) are only allowed for Data Principal - invalid channels/methods are ignored
 * 2. Methods (CALLBACK) are only allowed for Data Fiduciary and Data Processors - invalid channels/methods are ignored
 *
 * This service follows a graceful approach where invalid configurations are ignored rather than rejected.
 */
@Service
@Slf4j
public class EventConfigurationValidationService {

    private static final List<String> ALLOWED_CHANNELS = List.of("SMS", "EMAIL");
    private static final List<String> ALLOWED_METHODS = List.of("CALLBACK");

    /**
     * Sanitizes the event configuration request according to business rules.
     * Invalid configurations are removed/ignored with appropriate logging.
     *
     * @param request The event configuration request to sanitize
     * @throws ValidationException only for critical validation failures (e.g., missing notifications)
     */
    public void validateEventConfiguration(CreateEventConfigurationRequestDto request) {
        log.debug("Sanitizing event configuration for eventType: {}", request.getEventType());

        if (request.getNotifications() == null) {
            throw new ValidationException("Notifications configuration is required");
        }

        CreateEventConfigurationRequestDto.NotificationsConfigDto notifications = request.getNotifications();

        // Sanitize Data Principal - channels only
        sanitizeDataPrincipal(notifications.getDataPrincipal());

        // Sanitize Data Fiduciary - methods only
        sanitizeDataFiduciary(notifications.getDataFiduciary());

        // Sanitize Data Processor - methods only
        sanitizeDataProcessor(notifications.getDataProcessor());

        log.debug("Event configuration sanitization completed for eventType: {}", request.getEventType());
    }

    private void sanitizeDataPrincipal(CreateEventConfigurationRequestDto.RecipientConfigDto dataPrincipal) {
        if (dataPrincipal == null || !Boolean.TRUE.equals(dataPrincipal.getEnabled())) {
            return; // Skip sanitization if not enabled
        }

        // Data Principal can have channels but no methods - remove method if present
        if (dataPrincipal.getMethod() != null && !dataPrincipal.getMethod().trim().isEmpty()) {
            log.warn("Ignoring method '{}' for Data Principal. Methods are only allowed for Data Fiduciary and Data Processors.",
                    dataPrincipal.getMethod());
            dataPrincipal.setMethod(null);
        }

        // Filter out invalid channels if present
        if (dataPrincipal.getChannels() != null && !dataPrincipal.getChannels().isEmpty()) {
            List<String> validChannels = new ArrayList<>();
            for (String channel : dataPrincipal.getChannels()) {
                if (ALLOWED_CHANNELS.contains(channel)) {
                    validChannels.add(channel);
                } else {
                    log.warn("Ignoring invalid channel '{}' for Data Principal. Allowed channels: {}",
                            channel, ALLOWED_CHANNELS);
                }
            }
            dataPrincipal.setChannels(validChannels.isEmpty() ? null : validChannels);
        }
    }

    private void sanitizeDataFiduciary(CreateEventConfigurationRequestDto.RecipientConfigDto dataFiduciary) {
        if (dataFiduciary == null || !Boolean.TRUE.equals(dataFiduciary.getEnabled())) {
            return; // Skip sanitization if not enabled
        }

        // Data Fiduciary can have methods but no channels - remove channels if present
        if (dataFiduciary.getChannels() != null && !dataFiduciary.getChannels().isEmpty()) {
            log.warn("Ignoring channels {} for Data Fiduciary. Channels are only allowed for Data Principal.",
                    dataFiduciary.getChannels());
            dataFiduciary.setChannels(null);
        }

        // Validate and filter method if present
        if (dataFiduciary.getMethod() != null && !dataFiduciary.getMethod().trim().isEmpty()) {
            if (!ALLOWED_METHODS.contains(dataFiduciary.getMethod())) {
                log.warn("Ignoring invalid method '{}' for Data Fiduciary. Allowed methods: {}",
                        dataFiduciary.getMethod(), ALLOWED_METHODS);
                dataFiduciary.setMethod(null);
            }
        }
    }

    private void sanitizeDataProcessor(CreateEventConfigurationRequestDto.RecipientConfigDto dataProcessor) {
        if (dataProcessor == null || !Boolean.TRUE.equals(dataProcessor.getEnabled())) {
            return; // Skip sanitization if not enabled
        }

        // Data Processor can have methods but no channels - remove channels if present
        if (dataProcessor.getChannels() != null && !dataProcessor.getChannels().isEmpty()) {
            log.warn("Ignoring channels {} for Data Processor. Channels are only allowed for Data Principal.",
                    dataProcessor.getChannels());
            dataProcessor.setChannels(null);
        }

        // Validate and filter method if present
        if (dataProcessor.getMethod() != null && !dataProcessor.getMethod().trim().isEmpty()) {
            if (!ALLOWED_METHODS.contains(dataProcessor.getMethod())) {
                log.warn("Ignoring invalid method '{}' for Data Processor. Allowed methods: {}",
                        dataProcessor.getMethod(), ALLOWED_METHODS);
                dataProcessor.setMethod(null);
            }
        }
    }
}