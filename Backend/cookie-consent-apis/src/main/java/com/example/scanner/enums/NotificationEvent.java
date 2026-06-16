package com.example.scanner.enums;


public enum NotificationEvent {
    TOKEN_VALIDATION_SUCCESS("COOKIE_CONSENT", "TOKEN_VALIDATION_SUCCESS", true, false, false, false, false),
    NEW_COOKIE_CONSENT_VERSION_CREATED("COOKIE_CONSENT", "NEW_COOKIE_CONSENT_VERSION_CREATED", true, false, false, false, false),
    COOKIE_CONSENT_CREATED("COOKIE_CONSENT", "COOKIE_CONSENT_CREATED", true, false, false, false, false),
    CONSENT_REVOKED("COOKIE_CONSENT", "CONSENT_REVOKED", true, false, false, false, false),
    COOKIE_CONSENT_HANDLE_CREATED("COOKIE_CONSENT", "COOKIE_CONSENT_HANDLE_CREATED", true, false, false, false, false);

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