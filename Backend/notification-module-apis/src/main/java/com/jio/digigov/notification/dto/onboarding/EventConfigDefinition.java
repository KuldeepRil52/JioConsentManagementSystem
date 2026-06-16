package com.jio.digigov.notification.dto.onboarding;

import com.jio.digigov.notification.enums.EventPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Definition for event configuration during onboarding.
 * Specifies notification routing for DF, DP, Data Principal, DPO, and CMS.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventConfigDefinition {

    /**
     * Event type (e.g., "CONSENT_CREATED", "GRIEVANCE_RAISED")
     */
    private String eventType;

    /**
     * Data Fiduciary (DF) notification enabled
     */
    private boolean notifyDataFiduciary;

    /**
     * Data Processor (DP) notification enabled
     */
    private boolean notifyDataProcessor;

    /**
     * Data Principal notification enabled
     */
    private boolean notifyDataPrincipal;

    /**
     * Data Protection Officer (DPO) notification enabled
     */
    private boolean notifyDpo;

    /**
     * Consent Management System (CMS) notification enabled
     */
    private boolean notifyCms;

    /**
     * Event priority (HIGH, MEDIUM, LOW)
     */
    private EventPriority priority;

    /**
     * Event description
     */
    private String description;
}
