package com.jio.consent.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;

@Component
public class RestApiManager {

    private final RestTemplate restTemplate;

    @Value("${digilocker.proxy.host:}")
    private String proxyHost;

    @Value("${digilocker.proxy.port:8080}")
    private int proxyPort;

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

    // Proxy-enabled methods for DigiLocker calls
    public <RES> ResponseEntity<RES> getWithProxy(
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
        
        RestTemplate template = getRestTemplateWithProxy();
        return template.exchange(url, HttpMethod.GET, entity, responseType);
    }

    public <REQ, RES> ResponseEntity<REQ> postWithProxy(
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
        
        RestTemplate template = getRestTemplateWithProxy();
        return template.exchange(url, HttpMethod.POST, entity, responseType);
    }

    private RestTemplate getRestTemplateWithProxy() {
        if (proxyHost != null && !proxyHost.trim().isEmpty()) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            requestFactory.setProxy(proxy);
            return new RestTemplate(requestFactory);
        }
        return new RestTemplate();
    }

}
