package com.jio.digigov.notification.dto.onboarding.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Simple DTO for master label data from JSON.
 *
 * @author DPDP Notification Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasterLabelDataFile {

    /**
     * Label name (e.g., MASTER_LABEL_USER_IDENTIFIER)
     */
    private String labelName;

    /**
     * Data source (PAYLOAD, DB, TOKEN, GENERATE)
     */
    private String dataSource;

    /**
     * JSON path to extract value from (for PAYLOAD/TOKEN)
     */
    private String path;

    /**
     * Default value if not found
     */
    private String defaultValue;

    /**
     * MongoDB collection name (for DB datasource)
     */
    private String collection;

    /**
     * Query to execute (for DB datasource)
     */
    private Map<String, Object> query;

    /**
     * Generator type (for GENERATE datasource)
     */
    private String generator;

    /**
     * Generator configuration (for GENERATE datasource)
     */
    private Map<String, Object> config;
}
