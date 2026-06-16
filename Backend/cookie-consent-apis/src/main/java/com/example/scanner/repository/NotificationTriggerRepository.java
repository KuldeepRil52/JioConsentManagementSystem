package com.example.scanner.repository;

import com.example.scanner.dto.NotificationTrigger;

public interface NotificationTriggerRepository {

    NotificationTrigger save(NotificationTrigger notificationTrigger, String tenantId);

}
