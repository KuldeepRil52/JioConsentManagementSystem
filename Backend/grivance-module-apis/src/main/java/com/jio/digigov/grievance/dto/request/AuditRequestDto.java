package com.jio.digigov.grievance.dto.request;

import lombok.*;
import java.util.Map;

/**
 * Generic Audit Request DTO
 *
 * Can be used for:
 *  - Grievance audit events (actionType = GrievanceStatus)
 *  - Grievance Template audit events (actionType = Status)
 *
 * @param <T> Enum type for actionType (GrievanceStatus or Status)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRequestDto<T extends Enum<T>> {

    /** Business identifier */
    private String businessId;

    /** Actor details — who performed the action */
    private Actor actor;

    /** Group of audit, e.g., "GRIEVANCE" */
    private String group;

    /** Component name, e.g., "GRIEVANCE" or "GRIEVANCE_TEMPLATE" */
    private String component;

    /** Enum representing the action type (Status / GrievanceStatus) */
    private Enum<?> actionType;

    /** Resource information — what entity is being audited */
    private Resource resource;

    /** Initiator of the action (DATA_PRINCIPAL / DATA_FIDUCIARY / SYSTEM) */
    private String initiator;

    /** Additional contextual information like IP, transactionId, etc. */
    private Map<String, Object> context;

    /** Extra metadata fields (e.g., legalEntityName, PAN, etc.) */
    private Map<String, Object> extra;

    /** Optional status field if needed */
    private String status;

    // ------------------------------------------------------------------------
    // Nested static DTOs
    // ------------------------------------------------------------------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Actor {
        /** ID of actor (userId / grievanceId / templateId, etc.) */
        private String id;

        /** Role of actor (DATA_PRINCIPAL / DATA_FIDUCIARY / SYSTEM) */
        private String role;

        /** Type of actor (USER / BUSINESS / SYSTEM) */
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        /** Resource type (GrievanceID / TemplateID / etc.) */
        private String type;

        /** Unique ID of the resource */
        private String id;
    }
}
