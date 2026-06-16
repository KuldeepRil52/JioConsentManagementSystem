package com.jio.digigov.notification.service.dpo;

import com.jio.digigov.notification.dto.request.dpo.CreateDpoConfigurationRequestDto;
import com.jio.digigov.notification.dto.request.dpo.UpdateDpoConfigurationRequestDto;
import com.jio.digigov.notification.dto.response.dpo.DpoConfigurationResponseDto;

/**
 * Service interface for managing Data Protection Officer (DPO) configurations.
 *
 * <p>This service handles CRUD operations for DPO configurations at the tenant level.
 * Each tenant can have only ONE active DPO configuration shared across all businesses.</p>
 *
 * <p><b>Key Constraints:</b></p>
 * <ul>
 *   <li>One DPO configuration per tenant (tenant-scoped, not business-scoped)</li>
 *   <li>Cannot create second DPO config if one already exists</li>
 *   <li>Update operation replaces existing configuration</li>
 *   <li>Delete operation removes the configuration</li>
 * </ul>
 *
 * <p><b>DPO Configuration Structure:</b></p>
 * <pre>
 * {
 *   "configurationJson": {
 *     "name": "Jane Doe",
 *     "email": "dpo@company.com",
 *     "mobile": "9876543210",
 *     "address": "Mumbai, Maharashtra"
 *   }
 * }
 * </pre>
 *
 * @author DPDP Notification Team
 * @version 1.0
 * @since 2025-10-24
 */
public interface DpoConfigurationService {

    /**
     * Creates a new DPO configuration for the tenant or business.
     *
     * <p>If businessId is provided, creates a BUSINESS-scoped DPO configuration.
     * If businessId is null, creates a TENANT-scoped DPO configuration.</p>
     *
     * @param tenantId The tenant identifier
     * @param businessId The business identifier (optional, null for TENANT scope)
     * @param request The DPO configuration details
     * @return The created DPO configuration
     * @throws com.jio.digigov.notification.exception.BusinessException if DPO config already exists
     */
    DpoConfigurationResponseDto createDpoConfiguration(String tenantId, String businessId, CreateDpoConfigurationRequestDto request);

    /**
     * Retrieves the DPO configuration for the tenant.
     *
     * @param tenantId The tenant identifier
     * @return The DPO configuration if found
     * @throws com.jio.digigov.notification.exception.BusinessException if no configuration exists
     */
    DpoConfigurationResponseDto getDpoConfiguration(String tenantId);

    /**
     * Updates the existing DPO configuration for the tenant.
     *
     * <p>This operation replaces the existing configuration with new values.</p>
     *
     * @param tenantId The tenant identifier
     * @param request The updated DPO configuration details
     * @return The updated DPO configuration
     * @throws com.jio.digigov.notification.exception.BusinessException if no configuration exists
     */
    DpoConfigurationResponseDto updateDpoConfiguration(String tenantId, UpdateDpoConfigurationRequestDto request);

    /**
     * Deletes the DPO configuration for the tenant.
     *
     * <p>This operation removes the configuration document entirely.</p>
     *
     * @param tenantId The tenant identifier
     * @throws com.jio.digigov.notification.exception.BusinessException if no configuration exists
     */
    void deleteDpoConfiguration(String tenantId);

    /**
     * Checks if a DPO configuration exists for the tenant.
     *
     * @param tenantId The tenant identifier
     * @return true if a DPO configuration exists, false otherwise
     */
    boolean existsDpoConfiguration(String tenantId);
}
