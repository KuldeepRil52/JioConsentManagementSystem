package com.jio.digigov.notification.controller;

import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.util.CircuitBreakerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check controller
 */
@Slf4j
@RestController
@RequestMapping("/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Service health and status endpoints")
public class HealthController extends BaseController {

    private final CircuitBreakerUtil circuitBreakerUtil;
    
    @Operation(
        summary = "Health Check",
        description = "Returns service health status"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping
    public ResponseEntity<StandardApiResponseDto<Map<String, Object>>> health(HttpServletRequest httpRequest) {
        Map<String, Object> healthData = new LinkedHashMap<>();
        healthData.put("status", "UP");
        healthData.put("service", "notification-service-v2");
        healthData.put("timestamp", LocalDateTime.now());
        healthData.put("version", "1.0.0");

        log.debug("Health check requested");

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<Map<String, Object>> response = StandardApiResponseDto.success(
            healthData,
            "Service is healthy"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Circuit Breaker Metrics",
        description = "Returns circuit breaker status and metrics for all services"
    )
    @ApiResponse(responseCode = "200", description = "Circuit breaker metrics retrieved successfully")
    @GetMapping("/circuit-breaker")
    public ResponseEntity<StandardApiResponseDto<Map<String, Object>>> circuitBreakerMetrics(
            HttpServletRequest httpRequest) {
        Map<String, Object> metricsData = new LinkedHashMap<>();
        metricsData.put("timestamp", LocalDateTime.now());
        metricsData.put("metrics", circuitBreakerUtil.getAllMetrics());

        log.debug("Circuit breaker metrics requested");

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<Map<String, Object>> response = StandardApiResponseDto.success(
            metricsData,
            "Circuit breaker metrics retrieved successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Reset Circuit Breaker",
        description = "Manually reset a circuit breaker for a specific service"
    )
    @ApiResponse(responseCode = "200", description = "Circuit breaker reset successfully")
    @PostMapping("/circuit-breaker/reset")
    public ResponseEntity<StandardApiResponseDto<Map<String, Object>>> resetCircuitBreaker(
            HttpServletRequest httpRequest,
            @RequestParam String serviceName) {
        circuitBreakerUtil.resetCircuitBreaker(serviceName);

        Map<String, Object> resetData = new LinkedHashMap<>();
        resetData.put("status", "SUCCESS");
        resetData.put("message", "Circuit breaker reset for service: " + serviceName);
        resetData.put("timestamp", LocalDateTime.now());

        log.info("Circuit breaker manually reset for service: {}", serviceName);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<Map<String, Object>> response = StandardApiResponseDto.success(
            resetData,
            "Circuit breaker reset successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }
}