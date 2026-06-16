package com.example.scanner.config;

import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.regex.Pattern;

@Component
@Order(0) // HIGHEST priority - runs before all other filters
public class PathTraversalSecurityFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(PathTraversalSecurityFilter.class);

    // Enhanced path traversal patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            ".*(\\.\\.[\\/\\\\]|\\.\\.%2[fF]|\\.\\.%5[cC]|%2[eE]%2[eE][\\/\\\\]|" +
                    "%2[eE]%2[eE]%2[fF]|%2[eE]%2[eE]%5[cC]|" +
                    "[\\/\\\\]etc[\\/\\\\]|[\\/\\\\]var[\\/\\\\]|[\\/\\\\]usr[\\/\\\\]|" +
                    "[\\/\\\\]home[\\/\\\\]|[\\/\\\\]root[\\/\\\\]|" +
                    "[cC]:|[cC]%3[aA]|" +
                    "\\.\\.\\.\\.[\\/\\\\]).*"
    );

    private final ObjectMapper objectMapper;

    public PathTraversalSecurityFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String requestURI = httpRequest.getRequestURI();
            String queryString = httpRequest.getQueryString();

            // Check URI for path traversal
            if (isPathTraversalAttempt(requestURI)) {
                log.warn("SECURITY ALERT: Path traversal attempt blocked");
                handlePathTraversalAttempt(httpRequest, httpResponse, requestURI);
                return;
            }

            // Check query parameters for path traversal
            if (queryString != null && isPathTraversalAttempt(queryString)) {
                log.warn("SECURITY ALERT: Path traversal in query parameters - ");
                handlePathTraversalAttempt(httpRequest, httpResponse, requestURI + "?" + queryString);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Enhanced path traversal detection
     */
    private boolean isPathTraversalAttempt(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // Normalize for case-insensitive checking
        String normalized = input.toLowerCase();

        // Check for various path traversal patterns
        return PATH_TRAVERSAL_PATTERN.matcher(normalized).matches() ||
                normalized.contains("../") ||
                normalized.contains("..\\") ||
                normalized.contains("%2e%2e") ||
                normalized.contains("....//") ||
                containsSystemPaths(normalized);
    }

    private boolean containsSystemPaths(String input) {
        String[] systemPaths = {
                "/etc/passwd", "/etc/shadow", "/etc/hosts", "/etc/hostname",
                "/var/log", "/var/www", "/usr/bin", "/home/", "/root/",
                "c:/windows", "c:/users", "c:/program", "/windows/system32"
        };

        for (String path : systemPaths) {
            if (input.contains(path)) {
                return true;
            }
        }
        return false;
    }

    private void handlePathTraversalAttempt(HttpServletRequest request, HttpServletResponse response, String maliciousUri)
            throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                "Invalid URL path. Path traversal attempts are not allowed.",
                String.format("SECURITY: Path traversal attempt detected in URI: %s from IP: %s",
                        maliciousUri, getClientIpAddress(request)),
                Instant.now(),
                request.getRequestURI()
        );

        // FORCE JSON response
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Add security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "X-Client-IP",
                "CF-Connecting-IP",
                "True-Client-IP"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}