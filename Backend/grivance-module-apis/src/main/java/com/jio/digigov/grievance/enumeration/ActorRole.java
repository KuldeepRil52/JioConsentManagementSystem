package com.jio.digigov.grievance.enumeration;

/**
 * Enum representing the role of the actor in a grievance or audit event.
 * - DATA_PRINCIPAL: The end user or citizen who raises or owns the data.
 * - DATA_FIDUCIARY: The organization or system responsible for processing the data.
 * - SYSTEM: Internal automated system triggers (optional future use).
 */
public enum ActorRole {
    DATA_PRINCIPAL,
    DATA_FIDUCIARY,
    SYSTEM
}
