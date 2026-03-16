package com.jio.digigov.notification.dto.onboarding.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple DTO for event configuration data from JSON.
 *
 * @author DPDP Notification Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventConfigDataFile {

    /**
     * Event type
     */
    private String eventType;

    /**
     * Whether to notify Data Fiduciary
     */
    private Boolean notifyDataFiduciary;

    /**
     * Whether to notify Data Processor
     */
    private Boolean notifyDataProcessor;

    /**
     * Whether to notify Data Principal
     */
    private Boolean notifyDataPrincipal;

    /**
     * Whether to notify Data Protection Officer
     */
    private Boolean notifyDpo;

    /**
     * Whether to notify CMS
     */
    private Boolean notifyCms;

    /**
     * Priority level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    private String priority;

    /**
     * Description
     */
    private String description;
}
