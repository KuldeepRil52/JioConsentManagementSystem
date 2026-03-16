package com.jio.digigov.fides.repository;

import com.jio.digigov.fides.entity.NotificationTrigger;

public interface NotificationTriggerRepository {

    NotificationTrigger save(NotificationTrigger notificationTrigger, String tenantId);

}
