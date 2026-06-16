package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.entity.NotificationTrigger;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.NotificationTriggerRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationTriggerRepositoryImpl implements NotificationTriggerRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public NotificationTriggerRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }


    @Override
    public NotificationTrigger save(NotificationTrigger notificationTrigger, String tenantId) {
        return tenantMongoTemplateProvider.getMongoTemplate(tenantId).save(notificationTrigger);
    }
}
