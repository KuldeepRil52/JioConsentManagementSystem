package com.example.scanner.repository.impl;

import com.example.scanner.config.MultiTenantMongoConfig;
import com.example.scanner.config.TenantContext;
import com.example.scanner.entity.CookieConsentHandle;
import com.example.scanner.repository.ConsentHandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ConsentHandleRepositoryImpl implements ConsentHandleRepository {

    private final MultiTenantMongoConfig mongoConfig;

    @Override
    public CookieConsentHandle save(CookieConsentHandle consentHandle, String tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            return tenantMongoTemplate.save(consentHandle);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public CookieConsentHandle getByConsentHandleId(String consentHandleId, String tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Criteria criteria = new Criteria();
            criteria.and("consentHandleId").is(consentHandleId);
            Query query = new Query();
            query.addCriteria(criteria);
            return tenantMongoTemplate.findOne(query, CookieConsentHandle.class);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public CookieConsentHandle findActiveConsentHandle(String deviceId, String url, String templateId,
                                                       int templateVersion, String tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Criteria criteria = new Criteria();
            criteria.and("customerIdentifiers.value").is(deviceId)
                    .and("url").is(url)
                    .and("templateId").is(templateId)
                    .and("templateVersion").is(templateVersion);

            Query query = new Query(criteria);
            query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
            query.limit(1);

            CookieConsentHandle existingHandle = tenantMongoTemplate.findOne(query, CookieConsentHandle.class);

            // Check if handle is not expired
            if (existingHandle != null && !existingHandle.isExpired()) {
                return existingHandle;
            }

            return null;
        } finally {
            TenantContext.clear();
        }
    }

}