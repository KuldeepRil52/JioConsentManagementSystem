package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.RolesAccess;
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
public class ProfileResponse {
    private String userId;
    private String username;
    private String tenantId;
    private String spocName ;
    private String totpSecret;
    private String email;
    private String mobile;
    private String designation;
    private String identityType;
    private String clientId;
    private String pan;
    private List<RoleInfo> roles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        private String roleId;
        private String role;
        private String businessId;
        private String description;

        private List<PermissionInfo> permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionInfo {
        private String componentId;
        private String componentName;
        private String componentUrl;
        private String displayLabel;
        private String section;
        private RolesAccess access;
    }
}

