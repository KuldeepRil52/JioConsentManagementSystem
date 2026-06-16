package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.RolesAccess;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("roles")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Role extends AbstractEntity{

    @JsonIgnore
    @Id
    private ObjectId id;
    private String roleId;
    private String role;
    private String description;
    private List<RolePermission> permissions;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RolePermission {
        @Id
        @JsonIgnore
        private ObjectId id;
        private String componentId;
        private String componentName;
        private String componentUrl;
        private String displayLabel;
        private String section;
        private RolesAccess access;
    }

}
