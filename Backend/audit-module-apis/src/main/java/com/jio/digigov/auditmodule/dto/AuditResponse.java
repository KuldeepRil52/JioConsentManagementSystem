package com.jio.digigov.auditmodule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API response returned after creating or fetching audit logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditResponse {
    private String id;                // Same as auditId (UUID)
    private String status;            // success/failure
    private String message;           // Additional info

    // Optional blockchain-related metadata for verification
    private String currentChainHash;
    private String encryptedReferenceId;
}