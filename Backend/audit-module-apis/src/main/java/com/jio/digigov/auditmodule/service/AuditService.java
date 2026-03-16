package com.jio.digigov.auditmodule.service;

import com.jio.digigov.auditmodule.dto.AuditRecordResponse;
import com.jio.digigov.auditmodule.dto.AuditRequest;
import com.jio.digigov.auditmodule.dto.AuditResponse;
import com.jio.digigov.auditmodule.dto.PagedResponse;

import java.util.Map;

/**
 * Service interface for handling audit events.
 * Defines methods for creating, retrieving, and counting audits.
 */
public interface AuditService {

    /**
     * Create and publish a new audit event.
     *
     * @param req audit request payload
     * @param tenantId   String tenantId
     * @param transactionId   String transactionId
     * @return audit response with status and Kafka metadata
     */
    AuditResponse createAudit(AuditRequest req, String tenantId, String transactionId);

    /**
     * Retrieve paginated list of audits with optional filters.
     *
     * @param params filter key-value pairs
     * @param businessId   String businessId
     * @param page   page index (zero-based)
     * @param size   page size
     * @return paginated response containing audit documents
     */
    PagedResponse<AuditRecordResponse> getAuditsPaged(Map<String, String> params, String businessId, Integer page, Integer size, String sort);

    /**
     * Count audits matching given filters (without fetching actual documents).
     *
     * @param params filter key-value pairs
     * @param businessId   String businessId
     * @return count of matching audit documents
     */
    long countAudits(Map<String, String> params, String businessId);

    Map<String, Object> getAuditByReferenceId(String tenantId, String businessId, String referenceId);

}
