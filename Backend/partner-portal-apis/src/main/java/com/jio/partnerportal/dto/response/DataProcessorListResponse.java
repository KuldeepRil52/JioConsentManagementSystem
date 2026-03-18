package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing list of data processors with their activities")
public class DataProcessorListResponse {

    @Schema(description = "List of data processors")
    private List<ProcessorData> processors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProcessorData {
        @Schema(description = "Processor name")
        private String processorName;
        
        @Schema(description = "Processor information with activities")
        private ProcessorInfo processorInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProcessorInfo {
        private String dataProcessorId;
        private String dataProcessorName;
        private String callbackUrl;
        private String details;
        private String businessId;
        private String scopeType;
        private String status;
        private Boolean isCrossBordered;
        
        @Schema(description = "List of processor activities")
        private List<ProcessorActivityData> processorActivities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProcessorActivityData {
        @Schema(description = "Activity name")
        private String activityName;
        
        @Schema(description = "Activity information with data items")
        private ActivityInfo activityInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ActivityInfo {
        private String processorActivityId;
        private String activityName;
        private String processorId;
        private String processorName;
        private String details;
        private String businessId;
        private String scopeType;
        private String status;
        
        @Schema(description = "List of data items from all data types")
        private List<String> dataItems;
    }
}

