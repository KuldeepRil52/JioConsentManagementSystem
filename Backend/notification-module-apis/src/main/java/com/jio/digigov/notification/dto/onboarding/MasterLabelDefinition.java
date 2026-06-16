package com.jio.digigov.notification.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Definition for a master label in the notification system.
 * Ported from setup_master_list.js script.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterLabelDefinition {

    /**
     * Master label identifier (e.g., "MASTER_LABEL_USER_IDENTIFIER")
     */
    private String labelName;

    /**
     * Data source type: PAYLOAD, DB, or GENERATE
     */
    private String dataSource;

    /**
     * JSON path for extracting value from payload
     * Example: "eventPayload.consentId" or "customerIdentifiers.value"
     */
    private String path;

    /**
     * Default value if extraction fails
     */
    private String defaultValue;

    /**
     * Database collection name (for DB data source)
     */
    private String collection;

    /**
     * Database query (for DB data source)
     */
    private Map<String, String> query;

    /**
     * Generator type (for GENERATE data source): UUID, TIMESTAMP
     */
    private String generator;

    /**
     * Generator configuration (for GENERATE data source)
     */
    private Map<String, String> config;
}
