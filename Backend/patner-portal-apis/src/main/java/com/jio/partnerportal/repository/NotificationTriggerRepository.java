package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.DataBreachReport.NotificationStatus;
import com.jio.partnerportal.entity.NotificationTrigger;

import java.util.List;

public interface NotificationTriggerRepository {

    NotificationTrigger save(NotificationTrigger notificationTrigger, String tenantId);

    NotificationTrigger findByTriggerId(String triggerId, String tenantId);

    List<NotificationTrigger> findByBusinessId(String businessId, String tenantId);

    List<NotificationTrigger> findByStatus(NotificationStatus status, String tenantId);
}

