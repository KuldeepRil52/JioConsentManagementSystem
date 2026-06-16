package com.jio.schedular.repositoryImpl;

import com.jio.schedular.constant.Constants;
import com.jio.schedular.entity.NotificationTrigger;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.NotificationTriggerRepository;
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
