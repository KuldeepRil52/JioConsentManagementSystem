package com.jio.partnerportal.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("retention_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetentionConfig extends AbstractEntity{
    @JsonIgnore
    @Id
    private ObjectId id;

    private String retentionId;
    private String businessId;

    private Retentions retentions;

    @Data
    public static class Retentions {

        private RetentionValue consent_artifact_retention;
        private RetentionValue cookie_consent_artifact_retention;
        private RetentionValue grievance_retention;
        private RetentionValue logs_retention;
        private RetentionValue data_retention;
    }

    @Data
    public static class RetentionValue {
        private int value;   // e.g., 2, 6, 3
        private String unit; // months | years
    }
}
