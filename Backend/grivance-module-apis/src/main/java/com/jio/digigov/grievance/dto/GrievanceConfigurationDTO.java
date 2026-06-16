package com.jio.digigov.grievance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceConfigurationDTO {

    private ConfigurationJson configurationJson;
    private String createdAt;
    private String updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigurationJson {
        private List<String> grievanceTypes;
        private String endpointUrl;
        private List<String> intakeMethods;
        private List<String> workflow;
        private Timeline slaTimeline;
        private Timeline escalationPolicy;
        private Timeline retentionPolicy;
        private CommunicationConfig communicationConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Timeline {
        private int value;
        private String unit; // e.g. "MONTHS", "YEARS"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommunicationConfig {
        private boolean sms;
        private boolean email;
    }
}
