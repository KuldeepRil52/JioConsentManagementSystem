package com.jio.partnerportal.client.wso2;

import com.jio.partnerportal.client.wso2.request.OnboardBusinessRequest;
import com.jio.partnerportal.client.wso2.request.OnboardDataProcessorRequest;
import com.jio.partnerportal.client.wso2.request.RegisterTenantRequest;
import com.jio.partnerportal.client.wso2.response.OnboardBusinessResponse;
import com.jio.partnerportal.client.wso2.response.OnboardDataProcessorResponse;
import com.jio.partnerportal.client.wso2.response.RegisterTenantResponse;
import com.jio.partnerportal.exception.PartnerPortalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class Wso2Manager extends Wso2ApiManager {

    public RegisterTenantResponse registerTenant(String tenantId, String tenantName) throws PartnerPortalException {
        Map<String, String> headers = new HashMap<>();
        RegisterTenantRequest request = RegisterTenantRequest.builder().tenantId(tenantId).tenantName(tenantName).build();
        try{
            ResponseEntity<RegisterTenantResponse> responseEntity = super.postRegisterTenant(headers, request, RegisterTenantResponse.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                return responseEntity.getBody();
            }
        } catch (Exception e) {
            throw e;
        }
        return null;
    }

    public OnboardBusinessResponse onboardBusiness(OnboardBusinessRequest onboardBusinessRequest) throws PartnerPortalException {
        Map<String, String> headers = new HashMap<>();
        try{
            ResponseEntity<OnboardBusinessResponse> responseEntity = super.onboardBusiness(headers, onboardBusinessRequest, OnboardBusinessResponse.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                return responseEntity.getBody();
            }
        } catch (Exception e) {
            throw e;
        }
        return null;
    }

    public OnboardDataProcessorResponse onboardDataProcessor(OnboardDataProcessorRequest onboardDataProcessorRequest) throws PartnerPortalException {
        Map<String, String> headers = new HashMap<>();
        try{
            ResponseEntity<OnboardDataProcessorResponse> responseEntity = super.onboardDataProcessor(headers, onboardDataProcessorRequest, OnboardDataProcessorResponse.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                return responseEntity.getBody();
            }
        } catch (Exception e) {
            throw e;
        }
        return null;
    }
}
