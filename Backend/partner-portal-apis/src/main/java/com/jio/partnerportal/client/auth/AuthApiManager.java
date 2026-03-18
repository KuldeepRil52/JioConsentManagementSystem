package com.jio.partnerportal.client.auth;

import com.jio.partnerportal.util.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthApiManager extends RestApiManager {

    @Value("${auth.service.base.url}")
    private String authBaseUrl;

    @Value("${auth.service.endpoints.token}")
    private String tokenEndpoint;

    public <REQ, RES> ResponseEntity<REQ> postToken(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.post(authBaseUrl, tokenEndpoint, headers, requestBody, responseType);
    }

}

