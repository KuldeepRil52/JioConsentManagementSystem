package com.jio.digigov.notification.enums;

/**
 * Enumeration for Data Processor status
 */
public enum DataProcessorStatus {
    ACTIVE,     // Data Processor is active and can receive notifications
    INACTIVE,   // Data Processor is temporarily inactive
    SUSPENDED   // Data Processor is suspended due to policy violations
}