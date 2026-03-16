package com.jio.consent.repository;

import com.jio.consent.entity.NotificationTrigger;

public interface NotificationTriggerRepository {

    NotificationTrigger save(NotificationTrigger notificationTrigger, String tenantId);

}
