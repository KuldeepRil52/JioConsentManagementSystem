package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.NotificationEvent;

import java.util.List;

public interface NotificationEventRepository {

    NotificationEvent save(NotificationEvent notificationEvent);

    NotificationEvent findByNotificationId(String notificationId);

    List<NotificationEvent> findByTenantId(String tenantId);

    List<NotificationEvent> findByStatus(String status);
}

