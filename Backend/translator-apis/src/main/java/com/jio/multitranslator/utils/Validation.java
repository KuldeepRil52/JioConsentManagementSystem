package com.jio.multitranslator.utils;


import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.constant.ErrorCodes;
import com.jio.multitranslator.exceptions.BodyValidationException;
import com.jio.multitranslator.repository.TenantRegistryRepository;
import com.jio.multitranslator.repository.TranslateConfigRepository;
import org.apache.logging.log4j.ThreadContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Slf4j
@Component
public class Validation {

    private final TenantRegistryRepository tenantRegistryRepository;
    private final TranslateConfigRepository translateConfigRepository;

    public Validation(TenantRegistryRepository tenantRegistryRepository, 
                     TranslateConfigRepository translateConfigRepository) {
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.translateConfigRepository = translateConfigRepository;
    }

    /**
     * Validates the tenant ID header.
     *
     * @throws BodyValidationException if tenant ID is missing or invalid
     */
    public void validateTenantHeader() throws BodyValidationException {
        String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
        log.debug("Validating tenant header - TenantId: {}", tenantId);
        
        if (ObjectUtils.isEmpty(tenantId)) {
            log.warn("Tenant ID header is empty or null");
            List<Map<String, String>> errors = new ArrayList<>(1);
            errors.add(createHeaderError(ErrorCodes.JCMPT001, Constants.TENANT_ID_HEADER));
            throw new BodyValidationException(errors);
        }
        
        if (!tenantRegistryRepository.existsByTenantId(tenantId)) {
            log.warn("Tenant ID not found in registry - TenantId: {}", tenantId);
            List<Map<String, String>> errors = new ArrayList<>(1);
            errors.add(createHeaderError(ErrorCodes.JCMPT001, Constants.TENANT_ID_HEADER));
            throw new BodyValidationException(errors);
        }
        
        log.debug("Tenant header validation passed - TenantId: {}", tenantId);
    }

    /**
     * Validates the transaction ID header format (must be a valid UUID).
     *
     * @param txn the transaction ID to validate
     * @throws BodyValidationException if transaction ID is missing or invalid format
     */
    public void validateTxnHeader(String txn) throws BodyValidationException {
        log.debug("Validating transaction header - TXN: {}", txn);
        
        if (ObjectUtils.isEmpty(txn)) {
            log.warn("Transaction ID header is empty or null");
            List<Map<String, String>> errors = new ArrayList<>(1);
            errors.add(createHeaderError(ErrorCodes.JCMPT003, Constants.TNX_ID));
            throw new BodyValidationException(errors);
        }
        
        if (!isValidUUID(txn)) {
            log.warn("Invalid UUID format for transaction ID - TXN: {}", txn);
            List<Map<String, String>> errors = new ArrayList<>(1);
            errors.add(createHeaderError(ErrorCodes.JCMPT003, Constants.TNX_ID));
            throw new BodyValidationException(errors);
        }
        
        log.debug("Transaction header validation passed - TXN: {}", txn);
    }

    /**
     * Validates the business ID header.
     *
     * @throws BodyValidationException if business ID is missing or invalid
     */
    public void validateBusinessIdHeader() throws BodyValidationException {
        String businessId = ThreadContext.get(Constants.BUSINESS_ID_HEADER);
        log.debug("Validating business ID header - BusinessId: {}", businessId);
        
        if (ObjectUtils.isEmpty(businessId)) {
            log.warn("Business ID header is empty or null");
            List<Map<String, String>> errors = new ArrayList<>(1);
            errors.add(createHeaderError(ErrorCodes.JCMPT002, Constants.BUSINESS_ID_HEADER));
            throw new BodyValidationException(errors);
        }
        
        if (!translateConfigRepository.checkBusinessExists(businessId)) {
            log.warn("Business ID not found in repository - BusinessId: {}", businessId);
            List<Map<String, String>> errors = new ArrayList<>(1);
            errors.add(createHeaderError(ErrorCodes.JCMPT002, Constants.BUSINESS_ID_HEADER));
            throw new BodyValidationException(errors);
        }
        
        log.debug("Business ID header validation passed - BusinessId: {}", businessId);
    }

    /**
     * Checks if a string is a valid UUID.
     */
    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Creates header error details map.
     */
    private Map<String, String> createHeaderError(String errorCode, String header) {
        Map<String, String> errorMap = HashMap.newHashMap(2);
        errorMap.put(Constants.ERROR_CODE, errorCode);
        errorMap.put(Constants.HEADER, header);
        return errorMap;
    }
}
