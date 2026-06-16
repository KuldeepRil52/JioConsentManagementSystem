package com.jio.digigov.grievance.integration.notification;

import com.jio.digigov.grievance.entity.Grievance;

public interface NotificationEventService {

    /**
     * Triggers the Notification API for a given grievance event.
     *
     * @param grievance  The grievance entity containing user details and status.
     * @param tenantId   The tenant ID header value.
     * @param businessId The business ID header value.
     * @param grievanceId The grievance unique ID.
     */
    void triggerNotification(Grievance grievance, String tenantId, String businessId, String grievanceId);
}