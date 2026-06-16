package com.example.scanner.repository.impl;

import com.example.scanner.config.MultiTenantMongoConfig;
import com.example.scanner.dto.NotificationTrigger;
import com.example.scanner.repository.NotificationTriggerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class NotificationTriggerRepositoryImpl implements NotificationTriggerRepository {

    private final MultiTenantMongoConfig mongoConfig;

    @Override
    public NotificationTrigger save(NotificationTrigger notificationTrigger, String tenantId) {
        return mongoConfig.getMongoTemplateForTenant(tenantId).save(notificationTrigger);
    }
}
