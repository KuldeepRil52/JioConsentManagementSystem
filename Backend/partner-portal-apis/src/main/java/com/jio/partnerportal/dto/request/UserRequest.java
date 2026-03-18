package com.jio.partnerportal.dto.request;

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
public class UserRequest {
    private String username;
    private String email;
    private String mobile;
    private String designation;
    private String identityType;
    private List<RoleRequest> roles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            name = "UserRoleRequest",
            description = "Role reference inside UserRequest"
    )
    public static class RoleRequest {
        private String roleId;
        private String businessId;
    }
}
