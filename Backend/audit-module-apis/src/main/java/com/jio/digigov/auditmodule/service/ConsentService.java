package com.jio.digigov.auditmodule.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public interface ConsentService {
    Map<String, Object> verifyConsentIntegrity(String tenantId, String businessId,
                                               String consentId, HttpServletResponse response, HttpServletRequest servletRequest) throws Exception;

    Map<String, Object> verifyConsentCookiesIntegrity(String tenantId, String businessId, String consentId, HttpServletResponse response, HttpServletRequest servletRequest) throws Exception;
}