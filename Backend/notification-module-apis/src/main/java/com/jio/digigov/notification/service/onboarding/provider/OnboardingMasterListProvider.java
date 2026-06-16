package com.jio.digigov.notification.service.onboarding.provider;

import com.jio.digigov.notification.dto.onboarding.MasterLabelDefinition;
import com.jio.digigov.notification.dto.onboarding.json.MasterLabelDataFile;
import com.jio.digigov.notification.service.onboarding.OnboardingDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides default master label definitions for the notification system.
 * Ported from setup_master_list.js script.
 *
 * This provider contains 40+ master label definitions organized by category:
 * - User Information
 * - Record Identifiers
 * - Dates/Timestamps
 * - Status and Details
 * - URLs and Links
 * - Organization Information
 * - System Headers
 * - Event Information
 * - Generated Values
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingMasterListProvider {

    private final OnboardingDataLoader dataLoader;

    /**
     * Loads master label definitions from JSON files.
     * This is the NEW approach - data from JSON.
     *
     * @return List of MasterLabelDefinition objects
     */
    public List<MasterLabelDefinition> getMasterLabelDefinitionsFromJson() {
        if (!dataLoader.isDataLoaded()) {
            log.warn("Onboarding data not loaded, falling back to hardcoded approach");
            return getAllMasterLabelDefinitions();
        }

        log.info("Loading master label definitions from JSON data...");
        List<MasterLabelDefinition> labels = new ArrayList<>();
        List<MasterLabelDataFile> masterLabelDataList = dataLoader.getAllMasterLabels();

        for (MasterLabelDataFile labelData : masterLabelDataList) {
            try {
                MasterLabelDefinition label = convertMasterLabelData(labelData);
                labels.add(label);
            } catch (Exception e) {
                log.error("Failed to convert master label {}", labelData.getLabelName());
            }
        }

        log.info("Loaded {} master label definitions from JSON data", labels.size());
        return labels;
    }

    /**
     * Converts master label data from JSON to MasterLabelDefinition.
     */
    private MasterLabelDefinition convertMasterLabelData(MasterLabelDataFile labelData) {
        return MasterLabelDefinition.builder()
                .labelName(labelData.getLabelName())
                .dataSource(labelData.getDataSource() != null ? labelData.getDataSource() : "PAYLOAD")
                .path(labelData.getPath())
                .defaultValue(labelData.getDefaultValue() != null ? labelData.getDefaultValue() : " ")
                .build();
    }

    /**
     * Returns all default master label definitions using the LEGACY hardcoded approach.
     * This method is kept for backward compatibility and fallback.
     *
     * @return List of MasterLabelDefinition objects
     */
    public List<MasterLabelDefinition> getAllMasterLabelDefinitions() {
        log.debug("Retrieving all master label definitions");

        List<MasterLabelDefinition> labels = new ArrayList<>();

        // User Information
        labels.addAll(getUserInformationLabels());

        // Record Identifiers
        labels.addAll(getRecordIdentifierLabels());

        // Dates/Timestamps
        labels.addAll(getDateTimestampLabels());

        // Status and Details
        labels.addAll(getStatusDetailsLabels());

        // URLs and Links
        labels.addAll(getUrlLabels());

        // Organization Information
        labels.addAll(getOrganizationLabels());

        // System Headers
        labels.addAll(getSystemHeaderLabels());

        // Event Information
        labels.addAll(getEventInformationLabels());

        // Generated Values
        labels.addAll(getGeneratedValueLabels());

        log.info("Loaded {} master label definitions", labels.size());

        return labels;
    }

    // ==================== USER INFORMATION ====================

    private List<MasterLabelDefinition> getUserInformationLabels() {
        return List.of(
                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_USER_IDENTIFIER")
                        .dataSource("PAYLOAD")
                        .path("customerIdentifiers.value")
                        .defaultValue("User")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_USER_MOBILE")
                        .dataSource("PAYLOAD")
                        .path("customerIdentifiers.value")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_USER_EMAIL")
                        .dataSource("PAYLOAD")
                        .path("customerIdentifiers.value")
                        .defaultValue(" ")
                        .build()
        );
    }

    // ==================== RECORD IDENTIFIERS ====================

    private List<MasterLabelDefinition> getRecordIdentifierLabels() {
        return List.of(
                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_CONSENT_ID")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.consentId")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_GRIEVANCE_ID")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.grievanceId")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_DATA_REF_ID")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.dataRefId")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_REFERENCE_ID")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.referenceId")
                        .defaultValue(" ")
                        .build()
        );
    }

    // ==================== DATES/TIMESTAMPS ====================

    private List<MasterLabelDefinition> getDateTimestampLabels() {
        return List.of(
                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_CREATED_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.createdAt")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_UPDATED_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.updatedAt")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_EXPIRY_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.expiryDate")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_RENEWAL_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.renewalDate")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_WITHDRAWN_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.withdrawnDate")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_RESOLVED_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.resolvedDate")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_CLOSED_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.closedDate")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_DELETED_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.deletedDate")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_SHARED_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.sharedDate")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_ESCALATED_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.escalatedDate")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_DENIED_DATE")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.deniedDate")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_EVENT_TIMESTAMP")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.timestamp")
                        .defaultValue(" ")
                        .build()
        );
    }

    // ==================== STATUS AND DETAILS ====================

    private List<MasterLabelDefinition> getStatusDetailsLabels() {
        return List.of(
                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_STATUS")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.status")
                        .defaultValue("Pending")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_STATUS_DETAILS")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.statusDetails")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_DENIAL_REASON")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.denialReason")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_RESOLUTION_DETAILS")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.resolutionDetails")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_THIRD_PARTY_NAME")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.thirdPartyName")
                        .defaultValue("Third Party")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_SHARED_WITH")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.sharedWith")
                        .defaultValue(" ")
                        .build()
        );
    }

    // ==================== URLS AND LINKS ====================

    private List<MasterLabelDefinition> getUrlLabels() {
        return List.of(
                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_ACTION_URL")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.actionUrl")
                        .defaultValue("#")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_TRACKING_URL")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.trackingUrl")
                        .defaultValue("#")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_REVIEW_URL")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.reviewUrl")
                        .defaultValue("#")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_RENEWAL_URL")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.renewalUrl")
                        .defaultValue("#")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_DASHBOARD_URL")
                        .dataSource("PAYLOAD")
                        .path("eventPayload.dashboardUrl")
                        .defaultValue("#")
                        .build()
        );
    }

    // ==================== ORGANIZATION INFORMATION ====================

    private List<MasterLabelDefinition> getOrganizationLabels() {
        return List.of(
                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_ORGANIZATION_NAME")
                        .dataSource("DB")
                        .collection("business_applications")
                        .query(Map.of("id", "{{header.X-Business-Id}}"))
                        .path("name")
                        .defaultValue("Organization")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_ORGANIZATION_ID")
                        .dataSource("PAYLOAD")
                        .path("header.X-Business-Id")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_DATA_FIDUCIARY_NAME")
                        .dataSource("DB")
                        .collection("business_applications")
                        .query(Map.of("id", "{{header.X-Business-Id}}"))
                        .path("name")
                        .defaultValue("Data Fiduciary")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_DATA_FIDUCIARY_ID")
                        .dataSource("PAYLOAD")
                        .path("header.X-Business-Id")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_BUSINESS_NAME")
                        .dataSource("DB")
                        .collection("business_applications")
                        .query(Map.of("id", "{{header.X-Business-Id}}"))
                        .path("name")
                        .defaultValue("Business")
                        .build()
        );
    }

    // ==================== SYSTEM HEADERS ====================

    private List<MasterLabelDefinition> getSystemHeaderLabels() {
        return List.of(
                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_TENANT_ID")
                        .dataSource("PAYLOAD")
                        .path("header.X-Tenant-Id")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_BUSINESS_ID")
                        .dataSource("PAYLOAD")
                        .path("header.X-Business-Id")
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_TRANSACTION_ID")
                        .dataSource("PAYLOAD")
                        .path("header.X-Transaction-Id")
                        .defaultValue(" ")
                        .build()
        );
    }

    // ==================== EVENT INFORMATION ====================

    private List<MasterLabelDefinition> getEventInformationLabels() {
        return List.of(
                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_EVENT_TYPE")
                        .dataSource("PAYLOAD")
                        .path("eventType")
                        .defaultValue("Event")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_EVENT_SOURCE")
                        .dataSource("PAYLOAD")
                        .path("source")
                        .defaultValue("System")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_LANGUAGE")
                        .dataSource("PAYLOAD")
                        .path("language")
                        .defaultValue("english")
                        .build()
        );
    }

    // ==================== GENERATED VALUES ====================

    private List<MasterLabelDefinition> getGeneratedValueLabels() {
        return List.of(
                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_CURRENT_TIMESTAMP")
                        .dataSource("GENERATE")
                        .generator("TIMESTAMP")
                        .config(Map.of("format", "ISO_8601"))
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_UNIQUE_ID")
                        .dataSource("GENERATE")
                        .generator("UUID")
                        .config(Map.of("version", "v4"))
                        .defaultValue(" ")
                        .build(),

                MasterLabelDefinition.builder()
                        .labelName("MASTER_LABEL_JWT_TOKEN")
                        .dataSource("GENERATE")
                        .generator("JWT_TOKEN")
                        .config(Map.of())
                        .defaultValue(" ")
                        .build()
        );
    }
}
