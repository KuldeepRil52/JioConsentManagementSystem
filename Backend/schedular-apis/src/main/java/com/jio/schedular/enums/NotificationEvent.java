package com.jio.schedular.enums;


import com.jio.schedular.constant.Constants;

public enum NotificationEvent {
    // CONSENT REQUEST
    CONSENT_REQUEST_PENDING(Constants.CONSENT_REQUEST, "CONSENT_REQUEST_PENDING", false, false, true, false, false),
    CONSENT_REQUEST_RENEWAL(Constants.CONSENT_REQUEST, "CONSENT_REQUEST_RENEWAL", false, false, true, false, false),
    CONSENT_REQUEST_EXPIRED(Constants.CONSENT_REQUEST, "CONSENT_REQUEST_EXPIRED", false, false, true, false, false),


    // CONSENT
    CONSENT_CREATED(Constants.CONSENT, "CONSENT_CREATED", true, true, true, false, false),
    CONSENT_UPDATED(Constants.CONSENT, "CONSENT_UPDATED", true, true, true, false, false),
    CONSENT_WITHDRAWN(Constants.CONSENT, "CONSENT_WITHDRAWN", true, true, true, false, false),
    CONSENT_EXPIRED(Constants.CONSENT, "CONSENT_EXPIRED", true, true, true, false, false),
    CONSENT_RENEWED(Constants.CONSENT, "CONSENT_RENEWED", true, true, true, false, false),
    CONSENT_PREFERENCE_EXPIRY(Constants.CONSENT, "CONSENT_PREFERENCE_EXPIRY", false, false, true, false, false),

    // GRIEVANCE
    GRIEVANCE_RAISED(Constants.GRIEVANCE, "GRIEVANCE_RAISED", true, false, true, true, false),
    GRIEVANCE_INPROCESS(Constants.GRIEVANCE, "GRIEVANCE_INPROCESS", true, false, true, true, false),
    GRIEVANCE_ESCALATED(Constants.GRIEVANCE, "GRIEVANCE_ESCALATED", true, false, true, true, false),
    GRIEVANCE_L1_ESCALATED(Constants.GRIEVANCE, "GRIEVANCE_L1_ESCALATED", true, false, true, true, false),
    GRIEVANCE_L2_ESCALATED(Constants.GRIEVANCE, "GRIEVANCE_L2_ESCALATED", true, false, true, true, false),
    GRIEVANCE_RESOLVED(Constants.GRIEVANCE, "GRIEVANCE_RESOLVED", true, false, true, true, false),
    GRIEVANCE_DENIED(Constants.GRIEVANCE, "GRIEVANCE_DENIED", true, false, true, true, false),
    GRIEVANCE_CLOSED(Constants.GRIEVANCE, "GRIEVANCE_CLOSED", true, false, true, true, false),
    GRIEVANCE_STATUS_UPDATED(Constants.GRIEVANCE, "GRIEVANCE_STATUS_UPDATED", false, false, true, true, true),

    // DATA
    DATA_DELETED("DATA", "DATA_DELETED", false, false, true, true, true),
    DATA_SHARED("DATA", "DATA_SHARED", false, false, true, true, true),

    // POLICY
    RETENTION_CONSENT_ARTIFACT_EXPIRED(Constants.POLICY, "RETENTION_CONSENT_ARTIFACT_EXPIRED", false, false, false, true, false),
    RETENTION_COOKIE_CONSENT_EXPIRED(Constants.POLICY, "RETENTION_COOKIE_CONSENT_EXPIRED", false, false, false, true, false),
    RETENTION_GRIEVANCE_EXPIRED(Constants.POLICY, "RETENTION_GRIEVANCE_EXPIRED", false, false, false, true, false),
    RETENTION_LOGS_EXPIRED(Constants.POLICY, "RETENTION_LOGS_EXPIRED", false, false, false, true, false),
    RETENTION_DATA_EXPIRED(Constants.POLICY, "RETENTION_DATA_EXPIRED", false, false, false, true, false),

    //COOKIE CONSENT
    COOKIE_CONSENT_EXPIRED("COOKIE_CONSENT", "COOKIE_CONSENT_EXPIRED", true, false, false, false, false),
    COOKIE_CONSENT_HANDLE_EXPIRED("COOKIE_CONSENT_HANDLE", "COOKIE_CONSENT_HANDLE_EXPIRED", true, false, false, false, false);

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
