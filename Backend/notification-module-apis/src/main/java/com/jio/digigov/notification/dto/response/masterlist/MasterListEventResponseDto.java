package com.jio.digigov.notification.dto.response.masterlist;

import com.jio.digigov.notification.enums.EventType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasterListEventResponseDto {

    private EventType eventType;
    private List<MasterLabelInfo> masterLabels;
    private int totalLabels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasterLabelInfo {
        private String label;
        private String description;
        private String dataSource;
        private boolean isOverride;
    }
}