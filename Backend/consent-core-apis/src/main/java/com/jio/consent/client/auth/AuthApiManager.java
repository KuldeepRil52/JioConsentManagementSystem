package com.jio.consent.client.auth;

import com.jio.consent.utils.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthApiManager extends RestApiManager {

    @Value("${auth.service.base.url}")
    private String authServiceBaseUrl;

    @Value("${auth.service.endpoints.createSecureCode}")
    private String createSecureCodeEndpoint;

    public <REQ, RES> ResponseEntity<RES> createSecureCode(Map<String, String> headers, REQ requestBody, Class<RES> responseType) {
        return super.post(authServiceBaseUrl, createSecureCodeEndpoint, headers, requestBody, responseType);
    }
}

