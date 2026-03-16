package com.jio.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class Wso2TokenService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${wso2.token.url:https://10.173.184.27:9443/oauth2/token}")
    private String TOKEN_URL;

    private static final String GRANT_TYPE_KEY = "grant_type";
    private static final String GRANT_TYPE_VALUE = "client_credentials";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded";
    private static final String SCOPE = "scope";


    public Map<String, Object> getAccessToken(String consumerKey, String consumerSecret) {
        String credentials = consumerKey + ":" + consumerSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_AUTHORIZATION, "Basic " + encodedCredentials);
        headers.set(HEADER_CONTENT_TYPE, CONTENT_TYPE_VALUE);
        String body = GRANT_TYPE_KEY + "=" + GRANT_TYPE_VALUE + "&scope=" + UUID.randomUUID().toString();

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST, request, Map.class
        );
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            log.debug("WSO2 Token API Response: " + response.getBody());
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to fetch WSO2 access token. Status: " + response.getStatusCode());
        }
    }
}
