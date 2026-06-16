package com.jio.digigov.notification.dto.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Audit Request DTO for sending audit events to central audit service.
 * This follows the standard audit service schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditRequestDto {

    @NotBlank(message = "Business ID is required")
    private String businessId;

    @NotNull(message = "Actor is required")
    private Actor actor;

    @NotBlank(message = "Group is required")
    private String group;

    @NotBlank(message = "Component is required")
    private String component;

    @NotBlank(message = "Action type is required")
    private String actionType;

    @NotNull(message = "Resource is required")
    private Resource resource;

    @NotBlank(message = "Initiator is required")
    private String initiator;

    private Map<String, Object> context;

    private Map<String, Object> extra;

    /**
     * Actor represents who performed the action
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Actor {
        @NotBlank(message = "Actor ID is required")
        private String id;

        @NotBlank(message = "Actor role is required")
        private String role;

        @NotBlank(message = "Actor type is required")
        private String type;
    }

    /**
     * Resource represents the resource being acted upon
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Resource {
        @NotBlank(message = "Resource type is required")
        private String type;

        @NotBlank(message = "Resource ID is required")
        private String id;
    }
}
