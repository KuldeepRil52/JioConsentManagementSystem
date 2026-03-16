package com.example.scanner.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
//@EnableWebMvc
@Component
public class RateLimitingConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingConfig.class);

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        log.info("Creating RateLimitInterceptor bean");
        return new RateLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/health",
                        "/metrics",
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/error"
                );
    }

    public Bucket createNewBucket() {
        // 100 requests per minute, with burst of 10
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        Bandwidth burstLimit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .addLimit(burstLimit)
                .build();
    }

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }
}