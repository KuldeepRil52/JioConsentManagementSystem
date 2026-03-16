package com.example.scanner.enums;

/**
 * Enum to track version status for templates and consents
 * - ACTIVE: Current active version (only one per logical ID)
 * - UPDATED: Previous version that has been superseded
 */
public enum VersionStatus {
    ACTIVE,   // Current version in use
    UPDATED,   // Previous version (superseded)
    INACTIVE


}