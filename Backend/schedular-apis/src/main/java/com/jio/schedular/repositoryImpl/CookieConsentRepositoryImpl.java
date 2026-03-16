package com.jio.schedular.repositoryImpl;

import com.jio.schedular.constant.Constants;
import com.jio.schedular.enums.ConsentStatus;
import com.jio.schedular.enums.VersionStatus;
import com.jio.schedular.entity.CookieConsent;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.CookieConsentRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CookieConsentRepositoryImpl implements CookieConsentRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public CookieConsentRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public List<CookieConsent> findAndMarkExpiredConsents(LocalDateTime now) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(
                ThreadContext.get(Constants.TENANT_ID_HEADER));

        Query query = new Query();
        query.addCriteria(
                Criteria.where(Constants.CONSENT_STATUS).is(VersionStatus.ACTIVE)
                        .and(Constants.STATUS).is(ConsentStatus.ACTIVE)
                        .and(Constants.END_DATE).lt(now)
        );


        query.fields().include("consentId").include("businessId");
        List<CookieConsent> expiredConsents = mongoTemplate.find(query, CookieConsent.class);

        if (expiredConsents != null && !expiredConsents.isEmpty()) {
            Update update = new Update();
            update.set(Constants.STATUS, ConsentStatus.EXPIRED);
            update.set("updatedAt", Instant.now());

            Query updateQuery = new Query();
            updateQuery.addCriteria(
                    Criteria.where(Constants.CONSENT_STATUS).is(VersionStatus.ACTIVE)
                            .and(Constants.STATUS).is(ConsentStatus.ACTIVE)
                            .and(Constants.END_DATE).lt(now)
            );

            mongoTemplate.updateMulti(updateQuery, update, CookieConsent.class);
        }

        return expiredConsents;
    }

    @Override
    public List<CookieConsent> findExpiredConsents(LocalDateTime now) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(
                ThreadContext.get(Constants.TENANT_ID_HEADER));

        Query query = new Query();
        query.addCriteria(
                Criteria.where(Constants.CONSENT_STATUS).is(VersionStatus.ACTIVE)
                        .and(Constants.STATUS).is(ConsentStatus.ACTIVE)
                        .and(Constants.END_DATE).lt(now)
        );

        // Return full documents, not just consentId and businessId
        return mongoTemplate.find(query, CookieConsent.class);
    }

    @Override
    public Optional<CookieConsent> findById(String id, String tenantId) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));

        CookieConsent consent = mongoTemplate.findOne(query, CookieConsent.class);
        return Optional.ofNullable(consent);
    }

    @Override
    public void saveToDatabase(CookieConsent consent, String tenantId) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(tenantId);
        mongoTemplate.save(consent);
    }
}