package com.jio.digigov.notification.entity.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.notification.entity.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * RetentionConfig entity representing retention configuration in the retention_config collection.
 * Used to retrieve data retention policy for the consent deletion details API.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "retention_config")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetentionConfig extends BaseEntity {

    @Field("retentionId")
    private String retentionId;

    @Indexed
    @Field("businessId")
    private String businessId;

    @Field("retentions")
    private Retentions retentions;

    /**
     * Nested class containing all retention policies.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Retentions {

        @Field("consent_artifact_retention")
        private RetentionPolicy consentArtifactRetention;

        @Field("cookie_consent_artifact_retention")
        private RetentionPolicy cookieConsentArtifactRetention;

        @Field("grievance_retention")
        private RetentionPolicy grievanceRetention;

        @Field("logs_retention")
        private RetentionPolicy logsRetention;

        @Field("data_retention")
        private RetentionPolicy dataRetention;
    }

    /**
     * Retention policy specifying duration with value and unit.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RetentionPolicy {

        @Field("value")
        private Integer value;

        @Field("unit")
        private String unit;
    }
}
