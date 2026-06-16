package com.jio.schedular.repositoryImpl;

import com.jio.schedular.constant.Constants;
import com.jio.schedular.enums.CookieConsentHandleStatus;
import com.jio.schedular.entity.CookieConsentHandle;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.CookieConsentHandleRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class CookieConsentHandleRepositoryImpl implements CookieConsentHandleRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public CookieConsentHandleRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public List<CookieConsentHandle> findExpiredPendingHandles(Instant currentTime) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(
                ThreadContext.get(Constants.TENANT_ID_HEADER));

        Criteria criteria = new Criteria();
        criteria.and("status").is(CookieConsentHandleStatus.PENDING);
        criteria.and("expiresAt").lt(currentTime);

        Query query = new Query(criteria);
        return mongoTemplate.find(query, CookieConsentHandle.class);
    }

    @Override
    public int markHandlesAsExpired(List<String> handleIds, Instant updatedAt) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(
                ThreadContext.get(Constants.TENANT_ID_HEADER));

        Query query = new Query(Criteria.where("_id").in(handleIds));
        Update update = new Update();
        update.set("status", CookieConsentHandleStatus.REQ_EXPIRED);
        update.set("updatedAt", updatedAt);

        long modifiedCount = mongoTemplate.updateMulti(query, update, CookieConsentHandle.class).getModifiedCount();
        return (int) modifiedCount;
    }
}