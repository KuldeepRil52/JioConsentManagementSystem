package com.jio.partnerportal.client.vault;

import com.jio.partnerportal.client.vault.request.OnboardKeyRequest;
import com.jio.partnerportal.client.vault.request.SignRequest;
import com.jio.partnerportal.client.vault.response.OnboardKeyResponse;
import com.jio.partnerportal.client.vault.response.SignResponse;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.exception.PartnerPortalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class VaultManager extends VaultApiManager {

    public OnboardKeyResponse onboardKey(String tenantId, String businessId, OnboardKeyRequest onboardKeyRequest) throws PartnerPortalException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Tenant-Id", tenantId);
        headers.put("Business-Id", businessId);
        headers.put("Content-Type", "application/json");
        
        try {
            ResponseEntity<OnboardKeyResponse> responseEntity = super.postOnboardKey(headers, onboardKeyRequest, OnboardKeyResponse.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                return responseEntity.getBody();
            }
        } catch (Exception e) {
            log.error("Error while onboarding key to vault: {}", e.getMessage(), e);
            throw new PartnerPortalException(ErrorCodes.JCMP5001);
        }
        return null;
    }

    public SignResponse sign(String tenantId, String businessId, SignRequest signRequest) throws PartnerPortalException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Tenant-Id", tenantId);
        headers.put("Business-Id", businessId);
        headers.put("Content-Type", "application/json");
        
        try {
            ResponseEntity<SignResponse> responseEntity = super.postSign(headers, signRequest, SignResponse.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                return responseEntity.getBody();
            }
        } catch (Exception e) {
            log.error("Error while signing payload with vault: {}", e.getMessage(), e);
            throw new PartnerPortalException(ErrorCodes.JCMP5001);
        }
        return null;
    }
}

