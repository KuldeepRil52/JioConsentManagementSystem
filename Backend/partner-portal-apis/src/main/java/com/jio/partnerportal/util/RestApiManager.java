package com.jio.partnerportal.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class RestApiManager {
    private final RestTemplate restTemplate;

    public RestApiManager() {
        this.restTemplate = new RestTemplate();
    }

    public <RES> ResponseEntity<RES> get(
            String baseUrl,
            String endpoint,
            Map<String, String> headers,
            Class<RES> responseType
    ) {
        String url = baseUrl + endpoint;
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
        HttpEntity<Void> entity = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
    }

    public <REQ, RES> ResponseEntity<REQ> post(
            String baseUrl,
            String endpoint,
            Map<String, String> headers,
            RES requestBody,
            Class<REQ> responseType
    ) {
        String url = baseUrl + endpoint;
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
        HttpEntity<RES> entity = new HttpEntity<>(requestBody, httpHeaders);
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
    }
}
