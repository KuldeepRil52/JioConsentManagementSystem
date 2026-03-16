package com.jio.digigov.notification.service.callback;

import com.jio.digigov.notification.enums.RecipientType;
import com.jio.digigov.notification.entity.event.DataProcessor;
import com.jio.digigov.notification.exception.CallbackUrlNotFoundException;
import com.jio.digigov.notification.repository.event.DataProcessorRepository;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for resolving callback URLs based on recipient type and configuration.
 *
 * This service implements dynamic callback URL resolution for the notification system,
 * supporting both Data Fiduciary and Data Processor recipient types. It uses the
 * multi-tenant database approach to fetch callback URLs from the appropriate collections
 * based on the recipient type.
 *
 * Supported Recipient Types:
 * - DATA_FIDUCIARY / DF: Callback URL fetched from system_config collection (configurationJson.baseUrl)
 * - DATA_PROCESSOR / DP: Callback URL fetched from DataProcessor collection
 *
 * Multi-Tenant Support:
 * - Uses tenant-specific MongoTemplate instances for database access
 * - Maintains data isolation between different tenants
 * - Follows existing architectural patterns in the application
 *
 * Error Handling:
 * - Throws CallbackUrlNotFoundException when URL cannot be resolved
 * - Provides detailed error messages for debugging
 * - Logs resolution attempts for operational monitoring
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CallbackUrlResolutionService {

    private final DataProcessorRepository dataProcessorRepository;
    private final MongoTemplateProvider mongoTemplateProvider;

    /**
     * Resolves callback URL based on recipient type, ID, and business context.
     *
     * This method implements the core logic for dynamic callback URL resolution:
     * 1. For Data Fiduciary (DF) recipients: Fetches callback URL from NGConfiguration
     * 2. For Data Processor (DP) recipients: Fetches callback URL from DataProcessor collection
     *
     * The method uses the multi-tenant database approach by getting the appropriate
     * MongoTemplate instance for the tenant and then querying the relevant collection.
     *
     * @param recipientType The type of recipient (DATA_FIDUCIARY, DF, DATA_PROCESSOR, DP)
     * @param recipientId The unique identifier of the recipient
     * @param businessId The business ID for scoping the lookup
     * @param tenantId The tenant ID for database routing
     * @return The resolved callback URL
     * @throws CallbackUrlNotFoundException if callback URL cannot be found
     * @throws IllegalArgumentException if recipient type is invalid
     */
    public String resolveCallbackUrl(String recipientType, String recipientId,
                                    String businessId, String tenantId) {

        log.debug("Resolving callback URL for recipientType={}, recipientId={}, businessId={}, tenantId={}",
                 recipientType, recipientId, businessId, tenantId);

        if (recipientType == null || recipientType.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient type cannot be null or empty");
        }

        if (businessId == null || businessId.trim().isEmpty()) {
            throw new IllegalArgumentException("Business ID cannot be null or empty");
        }

        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        String normalizedRecipientType = recipientType.trim().toUpperCase();

        if (RecipientType.DATA_FIDUCIARY.name().equals(normalizedRecipientType) || "DF".equals(normalizedRecipientType)) {
            return resolveDataFiduciaryCallbackUrl(businessId, mongoTemplate);
        } else if (RecipientType.DATA_PROCESSOR.name().equals(normalizedRecipientType) || "DP".equals(normalizedRecipientType)) {
            return resolveDataProcessorCallbackUrl(recipientId, businessId, mongoTemplate);
        } else {
            throw new IllegalArgumentException("Invalid recipient type: " + recipientType +
                ". Supported types are: DATA_FIDUCIARY, DF, DATA_PROCESSOR, DP");
        }
    }

    /**
     * Resolves callback URL for Data Fiduciary recipients from system_configurations collection.
     * The callback URL is stored in configurationJson.baseUrl field.
     *
     * @param businessId The business ID to look up configuration
     * @param mongoTemplate The tenant-specific MongoTemplate
     * @return The callback URL from system_configurations
     * @throws CallbackUrlNotFoundException if configuration or callback URL not found
     */
    private String resolveDataFiduciaryCallbackUrl(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Resolving Data Fiduciary callback URL for businessId={} from system_configurations collection", businessId);

        try {
            // Query system_configurations collection directly using MongoTemplate
            Query query = new Query(Criteria.where("businessId").is(businessId));

            // Use Document to avoid creating entity class for partner portal's SystemConfig
            Document systemConfig = mongoTemplate.findOne(query, Document.class, "system_configurations");

            if (systemConfig == null) {
                String errorMsg = String.format("SystemConfig not found in system_configurations collection for Data Fiduciary businessId: %s", businessId);
                log.error(errorMsg);
                throw new CallbackUrlNotFoundException(errorMsg);
            }

            // Extract baseUrl from configurationJson nested object
            Object configJsonObj = systemConfig.get("configurationJson");
            if (configJsonObj == null) {
                String errorMsg = String.format("configurationJson not found in SystemConfig for businessId: %s", businessId);
                log.error(errorMsg);
                throw new CallbackUrlNotFoundException(errorMsg);
            }

            Document configJson = (Document) configJsonObj;
            String callbackUrl = configJson.getString("baseUrl");

            if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
                String errorMsg = String.format("baseUrl not configured in SystemConfig for Data Fiduciary businessId: %s", businessId);
                log.error(errorMsg);
                throw new CallbackUrlNotFoundException(errorMsg);
            }

            log.info("Successfully resolved Data Fiduciary callback URL for businessId={}: {}", businessId, callbackUrl);
            return callbackUrl.trim();

        } catch (CallbackUrlNotFoundException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            String errorMsg = String.format("Error resolving Data Fiduciary callback URL for businessId: %s", businessId);
            log.error(errorMsg);
            throw new CallbackUrlNotFoundException(errorMsg, e);
        }
    }

    /**
     * Resolves callback URL for Data Processor recipients from DataProcessor collection.
     *
     * @param recipientId The data processor ID (dpId)
     * @param businessId The business ID for scoping
     * @param mongoTemplate The tenant-specific MongoTemplate
     * @return The callback URL from DataProcessor configuration
     * @throws CallbackUrlNotFoundException if processor or callback URL not found
     */
    private String resolveDataProcessorCallbackUrl(String recipientId, String businessId, MongoTemplate mongoTemplate) {
        log.debug("Resolving Data Processor callback URL for dpId={}, businessId={}", recipientId, businessId);

        if (recipientId == null || recipientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient ID cannot be null or empty for Data Processor");
        }

        try {
            Optional<DataProcessor> processorOpt = dataProcessorRepository
                .findByDataProcessorIdAndBusinessId(recipientId, businessId, mongoTemplate);

            if (processorOpt.isEmpty()) {
                String errorMsg = String.format("Data Processor not found for dpId: %s, businessId: %s", recipientId, businessId);
                log.error(errorMsg);
                throw new CallbackUrlNotFoundException(errorMsg);
            }

            DataProcessor processor = processorOpt.get();
            String callbackUrl = processor.getCallbackUrl();

            if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
                String errorMsg = String.format("Callback URL not configured for Data Processor dpId: %s, businessId: %s", recipientId, businessId);
                log.error(errorMsg);
                throw new CallbackUrlNotFoundException(errorMsg);
            }

            log.info("Successfully resolved Data Processor callback URL for dpId={}, businessId={}: {}",
                    recipientId, businessId, callbackUrl);
            return callbackUrl.trim();

        } catch (CallbackUrlNotFoundException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            String errorMsg = String.format("Error resolving Data Processor callback URL for dpId: %s, businessId: %s", recipientId, businessId);
            log.error(errorMsg);
            throw new CallbackUrlNotFoundException(errorMsg, e);
        }
    }

    /**
     * Validates if a callback URL can be resolved for the given parameters without actually resolving it.
     * Useful for pre-validation checks before processing notifications.
     *
     * @param recipientType The type of recipient
     * @param recipientId The recipient identifier
     * @param businessId The business ID
     * @param tenantId The tenant ID
     * @return true if callback URL can be resolved, false otherwise
     */
    public boolean canResolveCallbackUrl(String recipientType, String recipientId,
                                        String businessId, String tenantId) {
        try {
            resolveCallbackUrl(recipientType, recipientId, businessId, tenantId);
            return true;
        } catch (Exception e) {
            log.debug("Cannot resolve callback URL for recipientType={}, recipientId={}, businessId={}: {}",
                     recipientType, recipientId, businessId, e.getMessage());
            return false;
        }
    }
}