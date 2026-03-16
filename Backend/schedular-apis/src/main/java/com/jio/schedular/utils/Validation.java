package com.jio.schedular.utils;

import com.jio.schedular.constant.Constants;
import com.jio.schedular.constant.ErrorCodes;
import com.jio.schedular.exception.SchedularException;
import com.jio.schedular.repository.TenantRegistryRepository;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

@Component
public class Validation {

    TenantRegistryRepository tenantRegistryRepository;

    public Validation(TenantRegistryRepository tenantRegistryRepository) {
        this.tenantRegistryRepository = tenantRegistryRepository;
    }

    public void validateTenantIdHeader() throws SchedularException {
        if (ObjectUtils.isEmpty(ThreadContext.get(Constants.TENANT_ID_HEADER)) || !this.tenantRegistryRepository.existsByTenantId(ThreadContext.get(Constants.TENANT_ID_HEADER))) {
            throw new SchedularException(ErrorCodes.JCMP2001);
        }
    }

    public void validateTxnHeader(String txn) throws SchedularException {
        if (org.springframework.util.ObjectUtils.isEmpty(txn)) {
            throw new SchedularException(ErrorCodes.JCMP2001);
        } else {
            try {
                java.util.UUID.fromString(txn);
            } catch (IllegalArgumentException e) {
                throw new SchedularException(ErrorCodes.JCMP2001);
            }
        }
    }
}
