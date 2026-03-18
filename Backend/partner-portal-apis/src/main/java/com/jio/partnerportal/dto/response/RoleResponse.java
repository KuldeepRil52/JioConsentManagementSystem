package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.RolesAccess;
import com.jio.partnerportal.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleResponse {

    private String message;           // e.g. "Role created successfully"
    private String roleId;
    private String role;
    private String description;
    private List<Role.RolePermission> permissions;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionResponse {
        private String componentId;
        private String componentName;
        private String componentUrl;
        private String displayLabel;
        private String section;
        private RolesAccess access;
    }

}