package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.DataBreachReport.NotificationStatus;
import com.jio.partnerportal.entity.NotificationTriggerCentral;

import java.util.List;

public interface NotificationTriggerCentralRepository {

    NotificationTriggerCentral save(NotificationTriggerCentral notificationTriggerCentral);

    NotificationTriggerCentral findByTriggerId(String triggerId);

    List<NotificationTriggerCentral> findByBusinessId(String businessId);

    List<NotificationTriggerCentral> findByStatus(NotificationStatus status);
}

