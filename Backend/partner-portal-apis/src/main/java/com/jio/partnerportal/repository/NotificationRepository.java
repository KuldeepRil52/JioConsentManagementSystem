package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.NotificationConfig;

import java.util.List;
import java.util.Map;

public interface NotificationRepository {

    NotificationConfig save(NotificationConfig notificationConfig);

    NotificationConfig findByConfigId(String configId);

    NotificationConfig findByBusinessId(String businessId);

    List<NotificationConfig> findNotificationConfigByParams(Map<String, String> searchParams);

    long count();

    boolean existByScopeLevel(String scopeLevel);

    boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId);
}
