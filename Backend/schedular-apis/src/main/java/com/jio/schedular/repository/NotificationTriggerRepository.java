package com.jio.schedular.repository;

import com.jio.schedular.entity.NotificationTrigger;

public interface NotificationTriggerRepository {

    NotificationTrigger save(NotificationTrigger notificationTrigger, String tenantId);

}
