package com.jio.digigov.fides.util;

import com.jio.digigov.fides.constant.HeaderConstants;
import com.jio.digigov.fides.constant.ErrorCodes;
import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.entity.BusinessApplication;
import com.jio.digigov.fides.entity.TenantRegistry;
import com.jio.digigov.fides.exception.BodyValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HeaderValidationService {

    private final MongoTemplate sharedMongoTemplate;
    private final MultiTenantMongoConfig multiTenantMongoConfig;
    private MessageSource messageSource;

    public HeaderValidationService(
            @Qualifier("sharedMongoTemplate") MongoTemplate sharedMongoTemplate,
            MultiTenantMongoConfig multiTenantMongoConfig,
            MessageSource messageSource
    ) {
        this.sharedMongoTemplate = sharedMongoTemplate;
        this.multiTenantMongoConfig = multiTenantMongoConfig;
        this.messageSource = messageSource;
    }
    
    public void validateTenantAndBusinessId(String tenantId, String businessId) {

        // 1️⃣ Tenant header present
        if (ObjectUtils.isEmpty(tenantId)) {
            throw new BodyValidationException(ErrorCodes.JCMP1001);
        }

        // 2️⃣ Business header present
        if (ObjectUtils.isEmpty(businessId)) {
            throw new BodyValidationException(ErrorCodes.JCMP1002);
        }

        // 3️⃣ Validate tenant in shared DB
        Query tenantQuery = Query.query(Criteria.where("tenantId").is(tenantId));
        if (!sharedMongoTemplate.exists(tenantQuery, TenantRegistry.class)) {
            throw new BodyValidationException(ErrorCodes.JCMP1001);
        }

        // Set tenant context ONLY after tenant is valid
        ThreadContext.put(HeaderConstants.X_TENANT_ID, tenantId);
        TenantContextHolder.setTenantId(tenantId);

        // 4️⃣ Validate business in tenant DB
        try {
            MongoTemplate tenantTemplate =
                    multiTenantMongoConfig.getMongoTemplateForTenant(tenantId);

            Query businessQuery = Query.query(Criteria.where("businessId").is(businessId));
            if (!tenantTemplate.exists(businessQuery, BusinessApplication.class)) {
                throw new BodyValidationException(ErrorCodes.JCMP1002);
            }
        } catch (BodyValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new BodyValidationException(ErrorCodes.JCMP1002);
        }

        ThreadContext.put(HeaderConstants.X_BUSINESS_ID, businessId);
        TenantContextHolder.setBusinessId(businessId);
    }
}