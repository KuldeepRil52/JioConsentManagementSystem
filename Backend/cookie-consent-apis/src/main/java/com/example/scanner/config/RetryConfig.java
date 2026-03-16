package com.example.scanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RetryConfig {

    /**
     * Custom RetryTemplate for programmatic retry (optional)
     * This is useful if you want to use RetryTemplate directly instead of @Retryable
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Define which exceptions to retry and max attempts
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(org.springframework.web.client.RestClientException.class, true);
        retryableExceptions.put(org.springframework.web.client.ResourceAccessException.class, true);
        retryableExceptions.put(org.springframework.web.client.HttpServerErrorException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);  // Start with 1 second
        backOffPolicy.setMultiplier(2.0);        // Double each time
        backOffPolicy.setMaxInterval(10000);     // Max 10 seconds
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}