package com.jio.digigov.fides.repository.impl;

import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.entity.NotificationTrigger;
import com.jio.digigov.fides.repository.NotificationTriggerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationTriggerRepositoryImpl implements NotificationTriggerRepository {

    private final MultiTenantMongoConfig mongoConfig;

    @Autowired
    public NotificationTriggerRepositoryImpl(MultiTenantMongoConfig mongoConfig) {
        this.mongoConfig = mongoConfig;
    }


    @Override
    public NotificationTrigger save(NotificationTrigger notificationTrigger, String tenantId) {
        return mongoConfig.getMongoTemplateForTenant(tenantId).save(notificationTrigger);
    }
}
