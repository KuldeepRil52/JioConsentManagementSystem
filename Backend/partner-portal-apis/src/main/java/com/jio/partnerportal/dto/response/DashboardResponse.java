package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {

    @Schema(description = "Dashboard data organized by business name")
    private Map<String, BusinessDashboardData> dashboardData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BusinessDashboardData {
        @Schema(description = "Business information")
        private BusinessInfo businessInfo;
        
        @Schema(description = "List of processors for this business")
        private List<ProcessorData> processors;
        
        @Schema(description = "List of users for this business")
        private List<UserData> users;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BusinessInfo {
        private String businessId;
        private String name;
        private String description;
        private String scopeLevel;
    }

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserData {
        @Schema(description = "User ID")
        private String userId;
        
        @Schema(description = "User information with roles")
        private UserInfo userInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserInfo {
        private String username;
        private String email;
        private String mobile;
        private String designation;
        private String identityType;
        
        @Schema(description = "Map of roleId to role information")
        private Map<String, RoleInfo> roles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoleInfo {
        private String roleId;
        private String role;
        private String description;
        private String businessId;
        private List<RolePermissionInfo> permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RolePermissionInfo {
        private String componentId;
        private String componentName;
        private String componentUrl;
        private String displayLabel;
        private String section;
        private com.jio.partnerportal.dto.RolesAccess access;
    }
}

