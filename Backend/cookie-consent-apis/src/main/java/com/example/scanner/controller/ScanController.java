package com.example.scanner.controller;

import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.request.AddCookieRequest;
import com.example.scanner.dto.request.CookieUpdateRequest;
import com.example.scanner.dto.request.ScanRequestDto;
import com.example.scanner.dto.response.AddCookieResponse;
import com.example.scanner.dto.response.CookieUpdateResponse;
import com.example.scanner.dto.response.ErrorResponse;
import com.example.scanner.dto.response.ScanStatusResponse;
import com.example.scanner.entity.CookieEntity;
import com.example.scanner.entity.ScanResultEntity;
import com.example.scanner.exception.ScanExecutionException;
import com.example.scanner.exception.TransactionNotFoundException;
import com.example.scanner.exception.UrlValidationException;
import com.example.scanner.service.CookieService;
import com.example.scanner.service.ScanService;
import com.example.scanner.util.CommonUtil;
import com.example.scanner.util.UrlAndCookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.example.scanner.exception.CookieNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Tag(name = "Cookie Scanner", description = "DPDPA Compliant Cookie Scanning and Management APIs with Subdomain Support and Rate Limiting")
public class ScanController {

    private final CookieService cookieService;
    private final ScanService scanService;

    @Operation(
            summary = "Start Website Cookie Scan with Protection",
            description = """
                Initiates comprehensive cookie scan with rate limiting and circuit breaker protection.
                Subdomains must belong to same root domain. Returns transaction ID for tracking.
                
                Error Codes: R4001 (Empty URL), R4291 (Rate limit), R5031 (Service unavailable), R5000 (Internal)
                """,
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", required = true, example = "tpl_123e4567-CXXXXXX....")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = ScanRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Scan initiated successfully",
                            content = @Content(
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(value = """
                                        {"transactionId": "550e8400-e29b-41d4-a716-446655440000", "message": "Scan started for main URL and 2 subdomains", "mainUrl": "https://example.com", "subdomainCount": 2}
                                        """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid URL or subdomains provided",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Too many requests - rate limit exceeded",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "503",
                            description = "Service temporarily unavailable",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Failed to initiate scan",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanUrl(
            @Parameter(description = "Tenant ID", required = true, example = "tpl_123e4567-CXXXXXX....")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody ScanRequestDto scanRequest) throws UrlValidationException, ScanExecutionException {

        String url = scanRequest.getUrl();
        List<String> subdomains = scanRequest.getSubDomain();

        if (url == null || url.trim().isEmpty()) {
            throw new UrlValidationException(ErrorCodes.EMPTY_ERROR,
                    "URL is required and cannot be empty",
                    "ScanRequestDto validation failed: url field is null or empty"
            );
        }


        try {
            String transactionId;
            transactionId = scanService.startScan(tenantId, url, subdomains);

            String message = "Scan started for main URL";
            if (subdomains != null && !subdomains.isEmpty()) {
                message += " and " + subdomains.size() + " subdomain" +
                        (subdomains.size() > 1 ? "s" : "");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("transactionId", transactionId);
            response.put("message", message);
            response.put("mainUrl", url);
            response.put("subdomainCount", subdomains != null ? subdomains.size() : 0);

            return ResponseEntity.ok(response);

        } catch (UrlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ScanExecutionException("Failed to initiate scan: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get Scan Status and Results",
            description = """
                Retrieves scan status and detailed results using transaction ID.
                Includes cookies from main URL and all subdomains with attribution.
                
                Error Codes: R4001 (Invalid TxnID), R4041 (Not found), R5000 (Internal)
                """,
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", required = true, example = "tpl_123e4567-CXXXXXX...."),
                    @Parameter(name = "transactionId", description = "Transaction ID from scan", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Scan status retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ScanStatusResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid transaction ID",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Transaction ID not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<ScanStatusResponse> getStatus(
            @Parameter(description = "Tenant ID", required = true, example = "tpl_123e4567-CXXXXXX....")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("transactionId") String transactionId) throws TransactionNotFoundException, ScanExecutionException, UrlValidationException {

        if (!CommonUtil.isValidTransactionId(transactionId)) {
            throw new UrlValidationException(
                    ErrorCodes.VALIDATION_ERROR,
                    "Invalid transaction ID format",
                    "Transaction ID must be a valid UUID format. Received: " + transactionId
            );
        }

        try {
            Optional<ScanResultEntity> resultOpt = scanService.getScanResult(tenantId, transactionId);
            if (resultOpt.isEmpty()) {
                throw new TransactionNotFoundException(transactionId);
            }

            ScanResultEntity result = resultOpt.get();

            List<ScanStatusResponse.SubdomainCookieGroup> subdomains = new ArrayList<>();

            if (result.getCookiesBySubdomain() != null) {
                for (Map.Entry<String, List<CookieEntity>> entry : result.getCookiesBySubdomain().entrySet()) {
                    String subdomainName = entry.getKey();
                    List<CookieEntity> cookies = entry.getValue();
                    String subdomainUrl = "main".equals(subdomainName) ? result.getUrl() :
                            constructSubdomainUrl(result.getUrl(), subdomainName);

                    subdomains.add(new ScanStatusResponse.SubdomainCookieGroup(subdomainName, subdomainUrl, cookies));
                }

                subdomains.sort((a, b) -> {
                    if ("main".equals(a.getSubdomainName())) return -1;
                    if ("main".equals(b.getSubdomainName())) return 1;
                    return a.getSubdomainName().compareTo(b.getSubdomainName());
                });
            }

            // Create summary from all cookies
            List<CookieEntity> allCookies = result.getCookiesBySubdomain() != null ?
                    result.getCookiesBySubdomain().values().stream()
                            .flatMap(List::stream)
                            .toList() : new ArrayList<>();

            Map<String, Integer> bySource = allCookies.stream()
                    .collect(Collectors.groupingBy(
                            cookie -> cookie.getSource() != null ? cookie.getSource().name() : "UNKNOWN",
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                    ));

            Map<String, Integer> byCategory = allCookies.stream()
                    .collect(Collectors.groupingBy(
                            cookie -> cookie.getCategory() != null ? cookie.getCategory() : "uncategorized",
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                    ));

            ScanStatusResponse.ScanSummary summary = new ScanStatusResponse.ScanSummary(bySource, byCategory);

            ScanStatusResponse response = new ScanStatusResponse(
                    result.getTransactionId(),
                    result.getStatus(),
                    result.getUrl(),
                    subdomains,
                    summary
            );

            return ResponseEntity.ok(response);

        } catch (TransactionNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ScanExecutionException("Failed to retrieve scan status: " + e.getMessage());
        }
    }

    private String constructSubdomainUrl(String mainUrl, String subdomainName) {
        try {
            String rootDomain = UrlAndCookieUtil.extractRootDomain(mainUrl);
            String protocol = mainUrl.startsWith("https") ? "https" : "http";
            return protocol + "://" + subdomainName + "." + rootDomain;
        } catch (Exception e) {
            return mainUrl;
        }
    }

    @Operation(
            summary = "Update Cookie Information",
            description = """
                Updates category, description, domain, privacy policy URL, expires for specific cookie.
                
                Error Codes: R4001 (Invalid TxnID), R4041 (Cookie/Txn not found), R5000 (Internal)
                """,
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", required = true, example = "tpl_123e4567-CXXXXXX...."),
                    @Parameter(name = "transactionId", description = "Transaction ID from scan", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = CookieUpdateRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Cookie updated successfully",
                            content = @Content(schema = @Schema(implementation = CookieUpdateResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request or cookie not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Transaction not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PutMapping("/transaction/{transactionId}/cookie")
    public ResponseEntity<CookieUpdateResponse> updateCookie(
            @Parameter(description = "Tenant ID", required = true, example = "tpl_123e4567-CXXXXXX....")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("transactionId") String transactionId,
            @Valid @RequestBody CookieUpdateRequest updateRequest) throws ScanExecutionException, CookieNotFoundException, TransactionNotFoundException, UrlValidationException {

        if (!CommonUtil.isValidTransactionId(transactionId)) {
            throw new UrlValidationException(
                    ErrorCodes.VALIDATION_ERROR,
                    "Invalid transaction ID format",
                    "Transaction ID must be a valid UUID format. Received: " + transactionId
            );
        }

        try {
            CookieUpdateResponse response = cookieService.updateCookie(tenantId, transactionId, updateRequest);

            if (response.isUpdated()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (CookieNotFoundException | TransactionNotFoundException | UrlValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ScanExecutionException("Failed to update cookie: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Add Cookie to Scan Transaction",
            description = """
                Manually adds cookie to transaction. Cookie domain must belong to same root domain.
                
                Error Codes: R4001 (Invalid TxnID), R4041 (Txn not found), R4091 (Duplicate), R5000 (Internal)
                """,
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", required = true, example = "tpl_123e4567-CXXXXXX...."),
                    @Parameter(name = "transactionId", description = "Transaction ID from scan", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = AddCookieRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Cookie added successfully",
                            content = @Content(schema = @Schema(implementation = AddCookieResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or duplicate cookie",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Transaction not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/transaction/{transactionId}/cookies")
    public ResponseEntity<AddCookieResponse> addCookie(
            @Parameter(description = "Tenant ID", required = true, example = "tpl_123e4567-CXXXXXX....")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("transactionId") String transactionId,
            @Valid @RequestBody AddCookieRequest addRequest) throws ScanExecutionException, TransactionNotFoundException, UrlValidationException {

        if (!CommonUtil.isValidTransactionId(transactionId)) {
            throw new UrlValidationException(
                    ErrorCodes.VALIDATION_ERROR,
                    "Invalid transaction ID format",
                    "Transaction ID must be a valid UUID format. Received: " + transactionId
            );
        }

        if (transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is required and cannot be empty");
        }

        AddCookieResponse response = cookieService.addCookie(tenantId, transactionId, addRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Enhanced Health Check with Protection Status",
            description = "Enhanced health check with circuit breaker and rate limiting status",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Service is healthy",
                            content = @Content(schema = @Schema(implementation = Map.class))
                    )
            }
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {

        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "Cookie Scanner API");
        healthInfo.put("version", "2.1.0");
        healthInfo.put("timestamp", java.time.Instant.now().toString());

        Map<String, Object> features = new HashMap<>();
        features.put("subdomainScanning", true);
        features.put("cookieCategorization", true);
        features.put("consentHandling", true);
        features.put("iframeProcessing", true);
        features.put("rateLimiting", scanService != null);
        features.put("circuitBreaker", scanService != null);

        healthInfo.put("features", features);

        // Add protection status
        Map<String, Object> protection = new HashMap<>();
        protection.put("rateLimiting", scanService != null);
        protection.put("circuitBreaker", scanService != null);
        protection.put("bulkhead", true);
        healthInfo.put("protection", protection);

        return ResponseEntity.ok(healthInfo);
    }

}