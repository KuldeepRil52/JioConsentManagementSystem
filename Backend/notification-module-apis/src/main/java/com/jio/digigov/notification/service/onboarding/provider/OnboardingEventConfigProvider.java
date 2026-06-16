package com.jio.digigov.notification.service.onboarding.provider;

import com.jio.digigov.notification.dto.onboarding.EventConfigDefinition;
import com.jio.digigov.notification.dto.onboarding.json.EventConfigDataFile;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.service.onboarding.OnboardingDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides default event configuration definitions for all event types.
 * Ported from setup_event_configurations.js script.
 *
 * This provider contains 18 event configuration definitions specifying
 * notification routing for DF, DP, Data Principal, DPO, and CMS.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingEventConfigProvider {

    private final OnboardingDataLoader dataLoader;

    /**
     * Loads event configuration definitions from JSON files.
     * This is the NEW approach - data from JSON.
     *
     * @return List of EventConfigDefinition objects
     */
    public List<EventConfigDefinition> getEventConfigDefinitionsFromJson() {
        if (!dataLoader.isDataLoaded()) {
            log.warn("Onboarding data not loaded, falling back to hardcoded approach");
            return getAllEventConfigDefinitions();
        }

        log.info("Loading event configuration definitions from JSON data...");
        List<EventConfigDefinition> configs = new ArrayList<>();
        List<EventConfigDataFile> eventConfigDataList = dataLoader.getAllEventConfigs();

        for (EventConfigDataFile configData : eventConfigDataList) {
            try {
                EventConfigDefinition config = convertEventConfigData(configData);
                configs.add(config);
            } catch (Exception e) {
                log.error("Failed to convert event config for {}", configData.getEventType());
            }
        }

        log.info("Loaded {} event configuration definitions from JSON data", configs.size());
        return configs;
    }

    /**
     * Converts event config data from JSON to EventConfigDefinition.
     */
    private EventConfigDefinition convertEventConfigData(EventConfigDataFile configData) {
        EventPriority priority = EventPriority.MEDIUM; // default
        if (configData.getPriority() != null) {
            try {
                priority = EventPriority.valueOf(configData.getPriority().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid priority '{}' for event {}, defaulting to MEDIUM",
                        configData.getPriority(), configData.getEventType());
            }
        }

        return EventConfigDefinition.builder()
                .eventType(configData.getEventType())
                .notifyDataFiduciary(configData.getNotifyDataFiduciary() != null ? configData.getNotifyDataFiduciary() : false)
                .notifyDataProcessor(configData.getNotifyDataProcessor() != null ? configData.getNotifyDataProcessor() : false)
                .notifyDataPrincipal(configData.getNotifyDataPrincipal() != null ? configData.getNotifyDataPrincipal() : false)
                .notifyDpo(configData.getNotifyDpo() != null ? configData.getNotifyDpo() : false)
                .notifyCms(configData.getNotifyCms() != null ? configData.getNotifyCms() : false)
                .priority(priority)
                .description(configData.getDescription())
                .build();
    }

    /**
     * Returns all default event configuration definitions using the LEGACY hardcoded approach.
     * This method is kept for backward compatibility and fallback.
     *
     * @return List of EventConfigDefinition objects (18 configurations)
     */
    public List<EventConfigDefinition> getAllEventConfigDefinitions() {
        log.debug("Retrieving all event configuration definitions");

        List<EventConfigDefinition> configs = new ArrayList<>();

        // Consent Events (7)
        configs.add(createConsentRequestPendingConfig());
        configs.add(createConsentRenewalRequestConfig());
        configs.add(createConsentCreatedConfig());
        configs.add(createConsentUpdatedConfig());
        configs.add(createConsentWithdrawnConfig());
        configs.add(createConsentExpiredConfig());
        configs.add(createConsentRenewedConfig());

        // Grievance Events (7)
        configs.add(createGrievanceRaisedConfig());
        configs.add(createGrievanceInprocessConfig());
        configs.add(createGrievanceEscalatedConfig());
        configs.add(createGrievanceResolvedConfig());
        configs.add(createGrievanceDeniedConfig());
        configs.add(createGrievanceClosedConfig());
        configs.add(createGrievanceStatusUpdatedConfig());

        // Data Events (3)
        configs.add(createDataDeletedConfig());
        configs.add(createDataSharedConfig());
        configs.add(createDataBreachedConfig());

        // Policy Events (2)
        configs.add(createDataRetentionExpiredConfig());
        configs.add(createLogRetentionExpiredConfig());

        log.info("Loaded {} event configuration definitions", configs.size());

        return configs;
    }

    // ==================== CONSENT EVENTS ====================

    private EventConfigDefinition createConsentRequestPendingConfig() {
        return EventConfigDefinition.builder()
                .eventType("CONSENT_REQUEST_PENDING")
                .notifyDataFiduciary(false)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(false)
                .notifyCms(false)
                .priority(EventPriority.MEDIUM)
                .description("Notification when consent request is pending")
                .build();
    }

    private EventConfigDefinition createConsentRenewalRequestConfig() {
        return EventConfigDefinition.builder()
                .eventType("CONSENT_RENEWAL_REQUEST")
                .notifyDataFiduciary(false)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(false)
                .notifyCms(false)
                .priority(EventPriority.MEDIUM)
                .description("Notification when consent renewal is requested")
                .build();
    }

    private EventConfigDefinition createConsentCreatedConfig() {
        return EventConfigDefinition.builder()
                .eventType("CONSENT_CREATED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(true)
                .notifyDataPrincipal(true)
                .notifyDpo(false)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when consent is created")
                .build();
    }

    private EventConfigDefinition createConsentUpdatedConfig() {
        return EventConfigDefinition.builder()
                .eventType("CONSENT_UPDATED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(true)
                .notifyDataPrincipal(true)
                .notifyDpo(false)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when consent is updated")
                .build();
    }

    private EventConfigDefinition createConsentWithdrawnConfig() {
        return EventConfigDefinition.builder()
                .eventType("CONSENT_WITHDRAWN")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(true)
                .notifyDataPrincipal(true)
                .notifyDpo(false)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when consent is withdrawn")
                .build();
    }

    private EventConfigDefinition createConsentExpiredConfig() {
        return EventConfigDefinition.builder()
                .eventType("CONSENT_EXPIRED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(true)
                .notifyDataPrincipal(true)
                .notifyDpo(false)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when consent has expired")
                .build();
    }

    private EventConfigDefinition createConsentRenewedConfig() {
        return EventConfigDefinition.builder()
                .eventType("CONSENT_RENEWED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(true)
                .notifyDataPrincipal(true)
                .notifyDpo(false)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when consent is renewed")
                .build();
    }

    // ==================== GRIEVANCE EVENTS ====================

    private EventConfigDefinition createGrievanceRaisedConfig() {
        return EventConfigDefinition.builder()
                .eventType("GRIEVANCE_RAISED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(true)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when grievance is raised")
                .build();
    }

    private EventConfigDefinition createGrievanceInprocessConfig() {
        return EventConfigDefinition.builder()
                .eventType("GRIEVANCE_INPROCESS")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(true)
                .notifyCms(false)
                .priority(EventPriority.MEDIUM)
                .description("Notification when grievance is in process")
                .build();
    }

    private EventConfigDefinition createGrievanceEscalatedConfig() {
        return EventConfigDefinition.builder()
                .eventType("GRIEVANCE_ESCALATED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(true)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when grievance is escalated")
                .build();
    }

    private EventConfigDefinition createGrievanceResolvedConfig() {
        return EventConfigDefinition.builder()
                .eventType("GRIEVANCE_RESOLVED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(true)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when grievance is resolved")
                .build();
    }

    private EventConfigDefinition createGrievanceDeniedConfig() {
        return EventConfigDefinition.builder()
                .eventType("GRIEVANCE_DENIED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(true)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when grievance is denied")
                .build();
    }

    private EventConfigDefinition createGrievanceClosedConfig() {
        return EventConfigDefinition.builder()
                .eventType("GRIEVANCE_CLOSED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(true)
                .notifyCms(false)
                .priority(EventPriority.MEDIUM)
                .description("Notification when grievance is closed")
                .build();
    }

    private EventConfigDefinition createGrievanceStatusUpdatedConfig() {
        return EventConfigDefinition.builder()
                .eventType("GRIEVANCE_STATUS_UPDATED")
                .notifyDataFiduciary(false)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(true)
                .notifyCms(true)
                .priority(EventPriority.MEDIUM)
                .description("Notification when grievance status is updated")
                .build();
    }

    // ==================== DATA EVENTS ====================

    private EventConfigDefinition createDataDeletedConfig() {
        return EventConfigDefinition.builder()
                .eventType("DATA_DELETED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(true)
                .notifyCms(true)
                .priority(EventPriority.HIGH)
                .description("Notification when data is deleted")
                .build();
    }

    private EventConfigDefinition createDataSharedConfig() {
        return EventConfigDefinition.builder()
                .eventType("DATA_SHARED")
                .notifyDataFiduciary(true)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(true)
                .notifyCms(true)
                .priority(EventPriority.HIGH)
                .description("Notification when data is shared")
                .build();
    }

    private EventConfigDefinition createDataBreachedConfig() {
        return EventConfigDefinition.builder()
                .eventType("DATA_BREACHED")
                .notifyDataFiduciary(false)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(true)
                .notifyDpo(false)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when data breach is detected")
                .build();
    }

    // ==================== POLICY EVENTS ====================

    private EventConfigDefinition createDataRetentionExpiredConfig() {
        return EventConfigDefinition.builder()
                .eventType("DATA_RETENTION_DURATION_EXPIRED")
                .notifyDataFiduciary(false)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(false)
                .notifyDpo(true)
                .notifyCms(false)
                .priority(EventPriority.HIGH)
                .description("Notification when data retention duration expires")
                .build();
    }

    private EventConfigDefinition createLogRetentionExpiredConfig() {
        return EventConfigDefinition.builder()
                .eventType("LOG_RETENTION_DURATION_EXPIRED")
                .notifyDataFiduciary(false)
                .notifyDataProcessor(false)
                .notifyDataPrincipal(false)
                .notifyDpo(true)
                .notifyCms(false)
                .priority(EventPriority.MEDIUM)
                .description("Notification when log retention duration expires")
                .build();
    }
}
