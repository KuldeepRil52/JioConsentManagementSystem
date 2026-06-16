package com.example.scanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(allowCredentials);

        // Parse allowed origins from properties
        List<String> originsList = Arrays.asList(allowedOrigins.split(","));
        if (allowedOrigins.equals("*")) {
            config.addAllowedOriginPattern("*");
        } else {
            originsList.forEach(config::addAllowedOrigin);
        }

        // Parse allowed methods
        Arrays.asList(allowedMethods.split(",")).forEach(config::addAllowedMethod);

        // Parse allowed headers
        if (allowedHeaders.equals("*")) {
            config.addAllowedHeader("*");
        } else {
            Arrays.asList(allowedHeaders.split(",")).forEach(config::addAllowedHeader);
        }

        config.setExposedHeaders(Arrays.asList(
                "X-Tenant-ID",
                "Business-ID",
                "Content-Disposition"
        ));

        config.setMaxAge(maxAge);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}