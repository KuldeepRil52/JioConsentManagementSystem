package com.jio.digigov.fides.enumeration;


import com.jio.digigov.fides.constant.Constants;

public enum NotificationEvent {

    DATA_DELETION_NOTIFICATION(Constants.SYSTEM, "DATA_DELETED", false, false, true, false, false);

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
