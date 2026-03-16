package com.jio.partnerportal.dto.request;

import com.jio.partnerportal.entity.Role;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpdateRequest {
    private String role;
    private String description;
    private List<Role.RolePermission> permissions;
}

