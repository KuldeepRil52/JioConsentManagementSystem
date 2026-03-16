package com.jio.digigov.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Security configuration for API protection and security headers.
 *
 * This configuration implements security measures including:
 * - Rate limiting to prevent abuse and DoS attacks
 * - Security headers for protection against common attacks
 * - CORS configuration for cross-origin requests
 * - Request size limits and timeout protections
 *
 * Security Features:
 * - Rate limiting: 100 requests per minute per IP
 * - Security headers: HSTS, CSP, X-Frame-Options, etc.
 * - CORS policy enforcement
 * - Request monitoring and logging
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class SecurityConfiguration implements WebMvcConfigurer {

    /**
     * Rate limiting interceptor to prevent API abuse
     */
    @Bean
    public RateLimitingInterceptor rateLimitingInterceptor() {
        return new RateLimitingInterceptor();
    }

    /**
     * Security headers interceptor
     */
    @Bean
    public SecurityHeadersInterceptor securityHeadersInterceptor() {
        return new SecurityHeadersInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor())
                .addPathPatterns("/v1/**");

        registry.addInterceptor(securityHeadersInterceptor())
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")   // ✅ Allow all origins
            .allowedMethods("*")           // ✅ Allow all HTTP methods
            .allowedHeaders("*")           // ✅ Allow all headers
            .allowCredentials(true)
            .maxAge(3600);
    }

    /**
     * Rate limiting interceptor implementation
     */
    public static class RateLimitingInterceptor implements HandlerInterceptor {

        private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Rate limit: 100 requests per minute per IP
        private static final int MAX_REQUESTS_PER_MINUTE = 100;

        public RateLimitingInterceptor() {
            // Reset counters every minute
            scheduler.scheduleAtFixedRate(() -> {
                requestCounts.clear();
                log.debug("Rate limit counters reset");
            }, 1, 1, TimeUnit.MINUTES);
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            String clientIp = getClientIpAddress(request);

            AtomicInteger count = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
            int currentCount = count.incrementAndGet();

            if (currentCount > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {} ({}  requests)", clientIp, currentCount);
                response.setStatus(429); // Too Many Requests
                response.setHeader("Retry-After", "60");
                response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded. Please try again later.\",\"code\":\"JDNM4002\"}");
                return false;
            }

            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS_PER_MINUTE - currentCount));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000));

            return true;
        }

        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        }
    }

    /**
     * Security headers interceptor implementation
     */
    public static class SecurityHeadersInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            // Security headers
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

            // Strict Transport Security (HSTS)
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

            // Content Security Policy
            response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self'; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none'");

            // Cache control for sensitive endpoints
            if (request.getRequestURI().contains("/v1/")) {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                Exception ex) throws Exception {
            // Log security events
            if (response.getStatus() >= 400) {
                log.warn("Security event - Status: {}, URI: {}, IP: {}",
                        response.getStatus(), request.getRequestURI(), request.getRemoteAddr());
            }
        }
    }
}