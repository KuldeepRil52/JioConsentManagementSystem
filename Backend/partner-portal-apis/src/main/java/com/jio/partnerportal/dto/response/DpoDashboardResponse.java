package com.jio.partnerportal.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DpoDashboardResponse {

    // -----------------------------------
    // Header
    // -----------------------------------
    private int totalPurposeCreated;
    private int dataTypes;
    private int processingActivities;
    private String dataretentionPeriod;
    private String logsretentionPeriod;
    private String consentArtefactsretentionPeriod;
    private int dataProcessor;

    // -----------------------------------
    // Templates
    // -----------------------------------
    private int publishedTemp;
    private int pendingRenewal;

    // -----------------------------------
    // Cookies (Status)
    // -----------------------------------
    private int cookiesPublished;
    private int cookiesDraft;
    private int cookiesInactive;

    // -----------------------------------
    // Consents
    // -----------------------------------
    private int totalConsents;
    private int activeConsents;
    private int revokedConsents;
    private int expiredConsents;
    private int autorenewalConsents;

    // -----------------------------------
    // Notifications
    // -----------------------------------
    private int notificationSent;
    private int notificationEmail;
    private int notificationSms;

    // -----------------------------------
    // Cookies (Actions)
    // -----------------------------------
    private int cookiesTotal;
    private int cookiesAllAccepted;
    private int cookiesPartiallyAccepted;
    private int cookieExpired;
    private int cookiesAllRejected;
    private int cookiesNoAction;

    // -----------------------------------
    // Grievances
    // -----------------------------------
    private int grievanceTotalRequests;
    private int grievanceResolved;
    private int grievanceInProgress;
    private int grievanceEscalatedL1;
    private int grievanceEscalatedL2;
    private int grievanceRejected;
    private int grievanceNew;

    // -----------------------------------
    // SLA
    // -----------------------------------
    private int resolvedSla;
    private int exceededSla;

    // -----------------------------------
    // Grievance Channels
    // -----------------------------------
    private int grievanceSms;
    private int grievanceEmail;
}

