package com.jio.digigov.notification.service.registry;

/**
 * Service interface for calling the consent withdraw API on System Registry.
 *
 * This API is called before DF (Data Fiduciary) callbacks for consent-related events
 * (CONSENT_EXPIRED, CONSENT_WITHDRAWN). The System Registry API saves withdrawal_data
 * directly to the notification_events collection.
 *
 * All operations are fire-and-forget to prevent blocking callback processing.
 */
public interface ConsentWithdrawService {

    /**
     * Calls the consent withdraw API for the given consent.
     *
     * This method is fire-and-forget - it does not return any data and does not
     * throw exceptions to the caller. The System Registry API saves the withdrawal
     * data directly to the notification_events collection.
     *
     * @param consentId   The consent ID to withdraw
     * @param eventId     The event ID triggering this withdrawal
     * @param tenantId    The tenant ID for headers
     * @param businessId  The business ID for headers
     */
    void withdrawConsent(String consentId, String eventId, String tenantId, String businessId);
}
