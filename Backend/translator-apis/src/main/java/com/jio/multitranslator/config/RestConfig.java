package com.jio.multitranslator.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for REST client and retry mechanisms.
 */
@Slf4j
@Configuration
public class RestConfig {

    private static final int DEFAULT_PROXY_PORT = 8080;
    private static final int DEFAULT_CONNECT_TIMEOUT = 10_000;
    private static final int DEFAULT_READ_TIMEOUT = 20_000;
    private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;
    private static final long DEFAULT_INITIAL_RETRY_INTERVAL = 500L;
    private static final double DEFAULT_RETRY_MULTIPLIER = 2.0;
    private static final long DEFAULT_MAX_RETRY_INTERVAL = 3_000L;

    @Value("${http.proxy.enabled:true}")
    private boolean proxyEnabled;

    @Value("${http.proxy.host:}")
    private String proxyHost;

    @Value("${http.proxy.port:" + DEFAULT_PROXY_PORT + "}")
    private int proxyPort;

    @Bean
    public RestTemplate restTemplate() {

        if (!proxyEnabled || proxyHost.isEmpty() || proxyPort <= 0) {
            // 🔹 No proxy – normal RestTemplate
            log.info("HTTP proxy DISABLED");
            return new RestTemplate();
        }

        // 🔹 Proxy enabled
        log.info("HTTP proxy ENABLED → {}:{}", proxyHost, proxyPort);
        HttpHost proxy = new HttpHost(proxyHost, proxyPort);

        HttpClient httpClient = HttpClientBuilder.create()
                .setProxy(proxy)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        factory.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        factory.setReadTimeout(DEFAULT_READ_TIMEOUT);

        return new RestTemplate(factory);
    }



    @Bean
    public RetryTemplate retryTemplate() {
        log.info("Configuring retry template - MaxAttempts: {}, InitialInterval: {}ms, Multiplier: {}, MaxInterval: {}ms",
                DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_INITIAL_RETRY_INTERVAL, 
                DEFAULT_RETRY_MULTIPLIER, DEFAULT_MAX_RETRY_INTERVAL);
        
        RetryTemplate retry = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(DEFAULT_MAX_RETRY_ATTEMPTS);
        retry.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(DEFAULT_INITIAL_RETRY_INTERVAL);
        backOff.setMultiplier(DEFAULT_RETRY_MULTIPLIER);
        backOff.setMaxInterval(DEFAULT_MAX_RETRY_INTERVAL);
        retry.setBackOffPolicy(backOff);

        return retry;
    }

    /**
     * Configures ObjectMapper bean for JSON serialization/deserialization.
     * This ensures consistent JSON handling across the application.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
