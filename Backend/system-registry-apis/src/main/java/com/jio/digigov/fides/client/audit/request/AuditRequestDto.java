package com.jio.digigov.fides.client.audit.request;

import lombok.*;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRequestDto {

    /** Business identifier */
    private String businessId;

    private Actor actor;

    private String group;

    private String component;

    private Enum<?> actionType;

    /** Resource information — what entity is being audited */
    private Resource resource;

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

        private String type;

        /** Unique ID of the resource */
        private String id;
    }
}
