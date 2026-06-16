package com.example.scanner.repository.impl;

import com.example.scanner.config.MultiTenantMongoConfig;
import com.example.scanner.config.TenantContext;
import com.example.scanner.constants.Constants;
import com.example.scanner.dto.CustomerIdentifiers;
import com.example.scanner.entity.CookieConsent;
import com.example.scanner.enums.VersionStatus;
import com.example.scanner.repository.ConsentRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentRepositoryCustomImpl implements ConsentRepositoryCustom {

    private final MultiTenantMongoConfig mongoConfig;

    @Override
    public CookieConsent saveToDatabase(CookieConsent consent, String tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            return tenantMongoTemplate.save(consent);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public CookieConsent existsByTemplateIdAndTemplateVersionAndCustomerIdentifiers(
            String templateId, Integer templateVersion, CustomerIdentifiers customerIdentifiers,
            String tenantId, String consentHandleId) {

        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Criteria criteria = new Criteria();
            criteria.and("templateId").is(templateId)
                    .and("templateVersion").is(templateVersion)
                    .and("customerIdentifiers.type").is(customerIdentifiers.getType())
                    .and("customerIdentifiers.value").is(customerIdentifiers.getValue())
                    .and("consentHandleId").is(consentHandleId);

            Query query = new Query(criteria);
            return tenantMongoTemplate.findOne(query, CookieConsent.class);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public CookieConsent findActiveByConsentId(String consentId, String tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where("consentId").is(consentId)
                    .and("consentStatus").is(VersionStatus.ACTIVE));

            return tenantMongoTemplate.findOne(query, CookieConsent.class);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public List<CookieConsent> findAllVersionsByConsentId(String consentId, String tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where("consentId").is(consentId))
                    .with(Sort.by(Sort.Direction.DESC, "version"));

            return tenantMongoTemplate.find(query, CookieConsent.class);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public Optional<CookieConsent> findByConsentIdAndVersion(String consentId, Integer version, String tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where("consentId").is(consentId)
                    .and("version").is(version));

            CookieConsent consent = tenantMongoTemplate.findOne(query, CookieConsent.class);
            return Optional.ofNullable(consent);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public Optional<CookieConsent> findById(String id, String tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            ObjectId objectId;
            if (ObjectId.isValid(id)) {
                objectId = new ObjectId(id);
            } else {
                throw new IllegalArgumentException("Invalid ObjectId: " + id);
            }

            Query query = new Query(Criteria.where("_id").is(objectId));

            CookieConsent consent = tenantMongoTemplate.findOne(query, CookieConsent.class);
            return Optional.ofNullable(consent);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public CookieConsent findLatestByCreatedAt() {
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(Constants.TENANT_ID_HEADER);
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        query.limit(1);
        return tenantMongoTemplate.findOne(query, CookieConsent.class);
    }
}