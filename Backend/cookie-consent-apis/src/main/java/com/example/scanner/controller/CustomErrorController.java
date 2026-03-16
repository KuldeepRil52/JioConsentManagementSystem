package com.example.scanner.controller;

import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request, HttpServletResponse response) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        String originalUri = requestUri != null ? requestUri.toString() : request.getRequestURI();

        // CRITICAL: Force JSON response
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String userMessage = "Invalid request format";
        String developerDetails = "Request processing failed";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // Enhanced path traversal detection
            if (originalUri != null && isPathTraversalAttempt(originalUri)) {
                userMessage = "Invalid URL path detected";
                developerDetails = String.format(
                        "SECURITY: Path traversal attempt blocked. URI: %s, Message: %s",
                        originalUri,
                        message != null ? message.toString() : "Invalid path format"
                );
            }
            else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                userMessage = "Invalid request format or characters in URL";
                developerDetails = String.format(
                        "Bad request for URI: %s. Error: %s. Exception: %s",
                        originalUri,
                        message != null ? message.toString() : "Invalid characters in URL",
                        exception != null ? exception.toString() : "none"
                );
            }
            else if (statusCode == HttpStatus.NOT_FOUND.value()) {
                userMessage = "The requested endpoint was not found";
                developerDetails = String.format("Not found: %s", originalUri);
                httpStatus = HttpStatus.NOT_FOUND;
            }
            else {
                httpStatus = HttpStatus.valueOf(statusCode);
                developerDetails = String.format("HTTP %d error for URI: %s", statusCode, originalUri);
            }
        }

        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                userMessage,
                developerDetails,
                Instant.now(),
                originalUri
        );

        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    /**
     * Enhanced path traversal detection
     */
    private boolean isPathTraversalAttempt(String uri) {
        if (uri == null) return false;

        String lowerUri = uri.toLowerCase();

        // Check for various path traversal patterns
        return lowerUri.contains("../") ||
                lowerUri.contains("..\\") ||
                lowerUri.contains("%2e%2e") ||      // URL-encoded ..
                lowerUri.contains("%2f") ||          // URL-encoded /
                lowerUri.contains("%5c") ||          // URL-encoded \
                lowerUri.contains("....//") ||       // Double encoded
                lowerUri.contains("..%2f") ||        // Mixed encoding
                lowerUri.contains("..%5c") ||        // Mixed encoding
                lowerUri.contains("/etc/") ||        // Direct system paths
                lowerUri.contains("/var/") ||
                lowerUri.contains("/usr/") ||
                lowerUri.contains("/home/") ||
                lowerUri.contains("/root/") ||
                lowerUri.contains("c:/") ||          // Windows paths
                lowerUri.contains("c:\\");
    }

    /**
     * Get client IP for security logging
     */
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