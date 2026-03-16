package com.jio.partnerportal.dto.request;

import lombok.Data;

@Data
public class RetentionRequest {

    private Retentions retentions;

    @Data
    public static class Retentions {

        private RetentionItem consent_artifact_retention;
        private RetentionItem cookie_consent_artifact_retention;
        private RetentionItem grievance_retention;
        private RetentionItem logs_retention;
        private RetentionItem data_retention;
    }

    @Data
    public static class RetentionItem {
        private String unit;   // DAYS / MONTHS / YEARS
        private Integer value; // how many

    }
}
