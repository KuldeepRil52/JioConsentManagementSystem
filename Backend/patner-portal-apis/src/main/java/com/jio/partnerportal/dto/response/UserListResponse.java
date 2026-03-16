package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.RolesAccess;
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
@Schema(description = "Response containing list of users with their roles")
public class UserListResponse {

    @Schema(description = "List of users")
    private List<UserData> users;

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
        private RolesAccess access;
    }
}

