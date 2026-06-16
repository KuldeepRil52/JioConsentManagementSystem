package com.jio.digigov.notification.enums;

public enum TemplateStatus {
    PENDING,   // Template onboarded but not approved
    ACTIVE,    // Template approved and ready for use
    INACTIVE,  // Template deactivated
    FAILED     // Template onboard/approve failed
}