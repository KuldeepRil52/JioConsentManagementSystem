package com.example.scanner.config;

import com.example.scanner.service.CookieCategorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(CookieCategorizationService.class);

    @Value("${cookie.categorization.api.connect.timeout:15000}")
    private int connectTimeout;

    @Value("${cookie.categorization.api.read.timeout:45000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout); // FROM PROPERTIES!
        factory.setReadTimeout(readTimeout);       // FROM PROPERTIES!

        RestTemplate restTemplate = new RestTemplate(factory);

        // Log for debugging
        log.info("RestTemplate configured - Connect: {}ms, Read: {}ms",
                connectTimeout, readTimeout);

        return restTemplate;
    }
}