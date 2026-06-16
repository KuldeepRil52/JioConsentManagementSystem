package com.jio.partnerportal.controller;

import com.jio.partnerportal.dto.request.RetentionRequest;
import com.jio.partnerportal.dto.response.RetentionResponse;
import com.jio.partnerportal.entity.RetentionConfig;
import com.jio.partnerportal.service.RetentionConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *
 * Retention Config Controller
 * @author Kirte
 *
 */
@RestController
@RequestMapping("/retention")
@RequiredArgsConstructor
public class RetentionConfigController {

    private final RetentionConfigService retentionConfigService;

    // *******************************************************************************************
    // CREATE RETENTION CONFIG
    // *******************************************************************************************

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Create a new retention config",
            description = "Creates retention configuration for a tenant and optionally business level",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Retention config request payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RetentionRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "businessId": "B001",
                                              "retentions": {
                                                  "consent_artifact_retention": { "value": 2, "unit": "years" },
                                                  "cookie_consent_artifact_retention": { "value": 6, "unit": "months" },
                                                  "grievance_retention": { "value": 3, "unit": "years" },
                                                  "logs_retention": { "value": 1, "unit": "years" },
                                                  "data_retention": { "value": 6, "unit": "months" }
                                              }
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Retention config created successfully",
                            content = @Content(schema = @Schema(implementation = RetentionResponse.class))
                    )
            }
    )
    @PostMapping("/create")
    public ResponseEntity<RetentionResponse> createRetentionConfig(
            @RequestHeader("txn") String txn,
            @RequestHeader("tenant-id") String tenantId,
            @RequestHeader("business-id") String businessId,
            @RequestHeader("x-session-token") String sessionToken,
            @RequestBody RetentionRequest request,
            HttpServletRequest req
    ) {
        return ResponseEntity.ok(
                retentionConfigService.createRetentionConfig(txn, tenantId, businessId, sessionToken, request, req)
        );
    }

    // *******************************************************************************************
    // UPDATE RETENTION CONFIG
    // *******************************************************************************************

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Update an existing retention config",
            description = "Updates the retention config identified by retentionId",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = RetentionRequest.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Retention config updated successfully",
                            content = @Content(schema = @Schema(implementation = RetentionResponse.class))
                    )
            }
    )
    @PutMapping("/update")
    public ResponseEntity<RetentionResponse> updateRetentionConfig(
            @RequestParam String retentionId,
            @RequestHeader("txn") String txn,
            @RequestHeader("tenant-id") String tenantId,
            @RequestHeader("business-id") String businessId,
            @RequestHeader("x-session-token") String sessionToken,
            @RequestBody RetentionRequest request,
            HttpServletRequest req
    ) {
        return ResponseEntity.ok(
                retentionConfigService.updateRetentionConfig(retentionId, txn, tenantId, businessId, sessionToken, request, req)
        );
    }

//    // *******************************************************************************************
//    // DELETE RETENTION CONFIG
//    // *******************************************************************************************
//
@CrossOrigin(origins = "*")
@Operation(
        summary = "Delete a retention config",
        description = "Deletes retention config by retentionId",
        responses = {
                @ApiResponse(responseCode = "200", description = "Retention config deleted successfully",
                        content = @Content(schema = @Schema(implementation = Map.class)))
        }
)
@DeleteMapping("/delete")
public ResponseEntity<Map<String, Object>> deleteRetentionConfig(
        @RequestHeader("txn") String txn,
        @RequestHeader("tenant-id") String tenantId,
        @RequestHeader("x-session-token") String sessionToken,
        @RequestParam("retentionId") String retentionId,
        HttpServletRequest req
) {
    return ResponseEntity.ok(
            retentionConfigService.deleteRetentionConfig(txn, tenantId, sessionToken, retentionId, req)
    );
}
//
//    // *******************************************************************************************
//    // SEARCH RETENTION CONFIG
//    // *******************************************************************************************

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Search retention config",
            description = "Search by retentionId or businessId",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Retention config fetched successfully",
                            content = @Content(schema = @Schema(implementation = RetentionConfig.class))
                    )
            }
    )
    @GetMapping("/search")
    public ResponseEntity<RetentionResponse> searchRetentionConfig(
            @RequestHeader("txn") String txn,
            @RequestHeader("tenant-id") String tenantId,
            @RequestHeader("business-id") String businessId,
            @RequestHeader("x-session-token") String sessionToken,
            @RequestParam(value = "retentionId", required = false) String retentionId,
            HttpServletRequest req
    ) {
        return ResponseEntity.ok(
                retentionConfigService.searchRetentionConfig(txn, tenantId, businessId, sessionToken, retentionId, req)
        );
    }
}
