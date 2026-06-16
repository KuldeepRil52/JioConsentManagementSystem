package com.jio.digigov.notification.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum EventType {
    // Consent Events
    CONSENT_REQUEST_PENDING("CONSENT_REQUEST_PENDING", "Consent request is pending approval"),
    CONSENT_RENEWAL_REQUEST("CONSENT_RENEWAL_REQUEST", "Consent renewal requested"),
    CONSENT_CREATED("CONSENT_CREATED", "New consent created"),
    CONSENT_UPDATED("CONSENT_UPDATED", "Consent information updated"),
    CONSENT_WITHDRAWN("CONSENT_WITHDRAWN", "Consent withdrawn by user"),
    CONSENT_EXPIRED("CONSENT_EXPIRED", "Consent has expired"),
    CONSENT_RENEWED("CONSENT_RENEWED", "Consent successfully renewed"),
    CONSENT_PREFERENCE_EXPIRY("CONSENT_PREFERENCE_EXPIRY", "Consent preference is due to expire"),

    // Grievance Events
    GRIEVANCE_RAISED("GRIEVANCE_RAISED", "New grievance raised"),
    GRIEVANCE_INPROCESS("GRIEVANCE_INPROCESS", "Grievance is being processed"),
    GRIEVANCE_ESCALATED("GRIEVANCE_ESCALATED", "Grievance escalated to higher authority"),
    GRIEVANCE_ESCALATED_L1("GRIEVANCE_ESCALATED_L1", "Grievance escalated to Level 1"),
    GRIEVANCE_ESCALATED_L2("GRIEVANCE_ESCALATED_L2", "Grievance escalated to Level 2"),
    GRIEVANCE_RESOLVED("GRIEVANCE_RESOLVED", "Grievance has been resolved"),
    GRIEVANCE_DENIED("GRIEVANCE_DENIED", "Grievance request denied"),
    GRIEVANCE_CLOSED("GRIEVANCE_CLOSED", "Grievance closed"),
    GRIEVANCE_STATUS_UPDATED("GRIEVANCE_STATUS_UPDATED", "Grievance status updated"),
    GRIEVANCE_RETENTION_DURATION_EXPIRED("GRIEVANCE_RETENTION_DURATION_EXPIRED", "Grievance retention duration expired"),

    // Data Events
    DATA_DELETED("DATA_DELETED", "User data deleted"),
    DATA_SHARED("DATA_SHARED", "Data shared with third party"),
    DATA_BREACHED("DATA_BREACHED", "Data breach detected"),
    DATA_EXPIRED("DATA_EXPIRED", "Data has expired"),
    DATA_RETENTION_DURATION_EXPIRED("DATA_RETENTION_DURATION_EXPIRED", "Data retention duration expired"),
    LOG_RETENTION_DURATION_EXPIRED("LOG_RETENTION_DURATION_EXPIRED", "Log retention duration expired"),

    // Consent Retention Events
    CONSENT_RETENTION_DURATION_EXPIRED("CONSENT_RETENTION_DURATION_EXPIRED", "Consent retention duration expired"),

    // Retention Period Expired Events
    RETENTION_CONSENT_ARTIFACT_EXPIRED("RETENTION_CONSENT_ARTIFACT_EXPIRED", "Consent artifact retention period expired"),
    RETENTION_COOKIE_CONSENT_EXPIRED("RETENTION_COOKIE_CONSENT_EXPIRED", "Cookie consent retention period expired"),
    RETENTION_GRIEVANCE_EXPIRED("RETENTION_GRIEVANCE_EXPIRED", "Grievance retention period expired"),
    RETENTION_LOGS_EXPIRED("RETENTION_LOGS_EXPIRED", "System logs retention period expired"),
    RETENTION_DATA_EXPIRED("RETENTION_DATA_EXPIRED", "Data retention period expired"),

    // Consent Integrity Verification Events
    CONSENT_INTEGRITY_VERIFICATION_FAILED("CONSENT_INTEGRITY_VERIFICATION_FAILED", "Consent integrity verification failed"),
    CONSENT_INTEGRITY_VERIFICATION_COMPLETED("CONSENT_INTEGRITY_VERIFICATION_COMPLETED", "Consent integrity verification completed successfully"),

    // System Events
    TENANT_ONBOARDED("TENANT_ONBOARDED", "New tenant onboarded to the system"),
    ORGANIZATION_ONBOARDING("ORGANIZATION_ONBOARDING", "New organization onboarded"),
    PROCESSOR_ONBOARDING("PROCESSOR_ONBOARDING", "New data processor onboarded"),
    DPO_USER_ONBOARDING("DPO_USER_ONBOARDING", "New DPO user account created"),

    // Authentication Events
    INIT_OTP("INIT_OTP", "OTP initialization for authentication or verification"),
    TOTP_PIN_DETAILS("TOTP_PIN_DETAILS", "TOTP setup details for 2FA");

    private final String value;
    private final String description;

    public static EventType fromValue(String value) {
        for (EventType eventType : values()) {
            if (eventType.value.equals(value)) {
                return eventType;
            }
        }
        throw new IllegalArgumentException("Invalid EventType: " + value);
    }

    public static boolean isValid(String value) {
        try {
            fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Set<EventType> getConsentEvents() {
        return Arrays.stream(values())
            .filter(event -> event.name().startsWith("CONSENT_"))
            .collect(Collectors.toSet());
    }

    public static Set<EventType> getGrievanceEvents() {
        return Arrays.stream(values())
            .filter(event -> event.name().startsWith("GRIEVANCE_"))
            .collect(Collectors.toSet());
    }

    public static Set<EventType> getDataEvents() {
        return Arrays.stream(values())
            .filter(event -> event.name().startsWith("DATA_"))
            .collect(Collectors.toSet());
    }
}