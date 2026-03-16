package com.jio.partnerportal.dto.request;
import com.jio.partnerportal.entity.Role;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleRequest {
    private String role;  // Enum as String
    private String description;
    private List<Role.RolePermission> permissions;

}
