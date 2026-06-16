package com.jio.partnerportal.client.auth;

import com.jio.partnerportal.client.auth.request.TokenRequest;
import com.jio.partnerportal.client.auth.response.TokenResponse;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.exception.PartnerPortalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AuthManager extends AuthApiManager {

    public TokenResponse getToken(String tenantId, String businessId, TokenRequest tokenRequest) throws PartnerPortalException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("accept", "*/*");
        
        try {
            ResponseEntity<TokenResponse> responseEntity = super.postToken(headers, tokenRequest, TokenResponse.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                return responseEntity.getBody();
            } else {
                log.error("Error calling Auth token rest API: Non-successful response status: {} for tenantId: {}, businessId: {}", 
                        responseEntity.getStatusCode(), tenantId, businessId);
                throw new PartnerPortalException(ErrorCodes.JCMP5003);
            }
        } catch (PartnerPortalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Auth token rest API for tenantId: {}, businessId: {}, error: {}", 
                    tenantId, businessId, e.getMessage(), e);
            throw new PartnerPortalException(ErrorCodes.JCMP5003);
        }
    }
}

