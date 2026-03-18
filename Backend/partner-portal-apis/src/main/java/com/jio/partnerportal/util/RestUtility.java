package com.jio.partnerportal.util;

import java.util.Map;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Kirte.Bhatt
 *
 */
@Service
@Slf4j
public class RestUtility {
   RestTemplate restTemplate;

    private RestUtility(RestTemplate restTemplate){
        this.restTemplate=restTemplate;
    }
    /**
     * Generic method for other API integration.
     */
    public ResponseEntity<String> callApi(String rawUrl, HttpMethod string, Map<String, Object> requestBody,
                                          Map<String, String> headers2) {
        log.debug("inside RestUtility callApi, url used: " + rawUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(headers2);

        HttpEntity<Object> entity;
        if (HttpMethod.GET.equals(string)) {
            entity = new HttpEntity<>(headers);
        } else {
            entity = new HttpEntity<>(requestBody, headers);
        }
        HttpMethod hm = string;
        ResponseEntity<String> res = restTemplate.exchange(rawUrl, hm, entity, String.class);
        log.debug("ResponseEntity {} response from Rest Utility ::::::::", res.toString());
        return res;
    }


    public ResponseEntity<String> callApiBypassSSL(String rawUrl, HttpMethod method,
                                                   Map<String, Object> requestBody, Map<String, String> headersMap) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(headersMap);

        HttpEntity<Object> entity = HttpMethod.GET.equals(method)
                ? new HttpEntity<>(headers)
                : new HttpEntity<>(requestBody, headers);

        RestTemplate unsafeRestTemplate = createUnsafeRestTemplate();

        return unsafeRestTemplate.exchange(rawUrl, method, entity, String.class);
    }

    private RestTemplate createUnsafeRestTemplate() {
        try {
            var sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();

            var sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            var connManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connManager)
                    .build();

            return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL bypass RestTemplate", e);
        }
    }

}











