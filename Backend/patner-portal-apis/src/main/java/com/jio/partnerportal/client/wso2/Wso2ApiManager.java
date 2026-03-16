package com.jio.partnerportal.client.wso2;

import com.jio.partnerportal.util.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Wso2ApiManager extends RestApiManager {

    @Value("${wso2.service.base.url}")
    private String wso2BaseUrl;

    @Value("${wso2.service.endpoints.register-tenant}")
    private String registerTenantEndpoint;

    @Value("${wso2.service.endpoints.onboard-business}")
    private String onboardBusinessEndpoint;

    @Value("${wso2.service.endpoints.onboard-dataprocessor}")
    private String onboardDataProcessorEndpoint;

    public <REQ, RES> ResponseEntity<REQ> postRegisterTenant(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.post(wso2BaseUrl, registerTenantEndpoint, headers, requestBody, responseType);
    }

    public <REQ, RES> ResponseEntity<REQ> onboardBusiness(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.post(wso2BaseUrl, onboardBusinessEndpoint, headers, requestBody, responseType);
    }

    public <REQ, RES> ResponseEntity<REQ> onboardDataProcessor(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.post(wso2BaseUrl, onboardDataProcessorEndpoint, headers, requestBody, responseType);
    }

}
