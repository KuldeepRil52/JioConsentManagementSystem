package com.jio.digigov.grievance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Value("${notification.trigger-url}")
    private String triggerUrl;

    @Value("${notification.audit-url}")
    private String auditUrl;

    public String getAuditUrl() {
        return auditUrl;
    }

    public String getTriggerUrl() {
        return triggerUrl;
    }
}

