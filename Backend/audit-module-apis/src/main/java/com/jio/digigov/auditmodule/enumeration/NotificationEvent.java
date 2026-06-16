package com.jio.digigov.auditmodule.enumeration;

public enum NotificationEvent {
    // CONSENT REQUEST
    CONSENT_REQUEST_PENDING("CONSENT REQUEST", "CONSENT_REQUEST_PENDING", false, false, true, false, false),
    CONSENT_RENEWAL_REQUEST("CONSENT REQUEST", "CONSENT_RENEWAL_REQUEST", false, false, true, false, false),

    // CONSENT
    CONSENT_CREATED("CONSENT", "CONSENT_CREATED", true, true, true, false, false),
    CONSENT_UPDATED("CONSENT", "CONSENT_UPDATED", true, true, true, false, false),
    CONSENT_WITHDRAWN("CONSENT", "CONSENT_WITHDRAWN", true, true, true, false, false),
    CONSENT_EXPIRED("CONSENT", "CONSENT_EXPIRED", true, true, true, false, false),
    CONSENT_RENEWED("CONSENT", "CONSENT_RENEWED", true, true, true, false, false),

    // CONSENT INTEGRITY VERIFICATION
    CONSENT_INTEGRITY_VERIFICATION_COMPLETED("CONSENT", "CONSENT_CREATED", false, false, false, true, false),
    CONSENT_INTEGRITY_VERIFICATION_FAILED("CONSENT", "CONSENT_UPDATED", false, false, false, true, false),

    // GRIEVANCE
    GRIEVANCE_RAISED("GRIEVANCE", "GRIEVANCE_RAISED", true, false, true, true, false),
    GRIEVANCE_INPROCESS("GRIEVANCE", "GRIEVANCE_INPROCESS", true, false, true, true, false),
    GRIEVANCE_ESCALATED("GRIEVANCE", "GRIEVANCE_ESCALATED", true, false, true, true, false),
    GRIEVANCE_RESOLVED("GRIEVANCE", "GRIEVANCE_RESOLVED", true, false, true, true, false),
    GRIEVANCE_DENIED("GRIEVANCE", "GRIEVANCE_DENIED", true, false, true, true, false),
    GRIEVANCE_CLOSED("GRIEVANCE", "GRIEVANCE_CLOSED", true, false, true, true, false),
    GRIEVANCE_STATUS_UPDATED("GRIEVANCE", "GRIEVANCE_STATUS_UPDATED", false, false, true, true, true),

    // DATA
    DATA_DELETED("DATA", "DATA_DELETED", false, false, true, true, true),
    DATA_SHARED("DATA", "DATA_SHARED", false, false, true, true, true),

    // POLICY
    DATA_RETENTION_DURATION_EXPIRED("POLICY", "DATA_RETENTION_DURATION_EXPIRED", false, false, false, true, false),
    LOG_RETENTION_DURATION_EXPIRED("POLICY", "LOG_RETENTION_DURATION_EXPIRED", false, false, false, true, false);



    private final String resource;
    private final String eventId;
    private final boolean df;
    private final boolean dp;
    private final boolean dataPrinciple;
    private final boolean dpo;
    private final boolean cms;

    NotificationEvent(String resource, String eventId, boolean df, boolean dp, boolean dataPrinciple, boolean dpo, boolean cms) {
        this.resource = resource;
        this.eventId = eventId;
        this.df = df;
        this.dp = dp;
        this.dataPrinciple = dataPrinciple;
        this.dpo = dpo;
        this.cms = cms;
    }

    public String getResource() { return resource; }
    public String getEventId() { return eventId; }
    public boolean isDf() { return df; }
    public boolean isDp() { return dp; }
    public boolean isDataPrinciple() { return dataPrinciple; }
    public boolean isDpo() { return dpo; }
    public boolean isCms() { return cms; }
}

