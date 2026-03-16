package com.jio.digigov.notification.controller;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.dto.masterlist.MasterListConfig;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.enums.MasterListDataSource;
import com.jio.digigov.notification.service.masterlist.MasterListConfigLoader;
import com.jio.digigov.notification.service.masterlist.TenantMasterListConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for master list functionality.
 * This controller provides test endpoints to demonstrate the master list
 * configuration loading with tenant-specific database fallback mechanism.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@RestController
@RequestMapping("/v1/test/master-lists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Master List Testing", description = "Test APIs for master list configuration and fallback mechanism")
public class MasterListTestController {

    private final MasterListConfigLoader configLoader;
    private final TenantMasterListConfigService tenantMasterListConfigService;

    @Operation(summary = "Test master list configuration loading",
               description = "Tests the master list configuration loading with tenant-specific database fallback")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Master list configuration loaded successfully"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid tenant ID header"),
        @ApiResponse(responseCode = "500", description = "Internal server error during configuration loading")
    })
    @GetMapping("/load/{businessId}")
    public ResponseEntity<MasterListConfig> testLoadConfiguration(
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @Parameter(description = "Business ID", required = true)
            @PathVariable String businessId) {

        log.info("Testing master list configuration loading for tenantId={}, businessId={}", tenantId, businessId);

        try {
            MasterListConfig config = configLoader.loadConfiguration(tenantId, businessId);
            log.info("Successfully loaded master list configuration from {} with {} entries",
                    config.getSource(), config.size());
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Error loading master list configuration: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Create sample tenant configuration",
               description = "Creates a sample tenant-specific master list configuration for testing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sample tenant configuration created successfully"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid tenant ID header"),
        @ApiResponse(responseCode = "500", description = "Internal server error during configuration creation")
    })
    @PostMapping("/sample")
    public ResponseEntity<String> createSampleTenantConfig(
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId) {

        log.info("Creating sample tenant master list configuration for tenantId={}", tenantId);

        try {
            log.info("Sample tenant configuration creation is no longer supported. Use POST /v1/master-lists instead.");
            return ResponseEntity.ok("Use POST /v1/master-lists API to create master list configuration");
        } catch (Exception e) {
            log.error("Error creating sample tenant configuration: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete tenant configuration",
               description = "Deletes the tenant-specific master list configuration to test fallback to static file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tenant configuration deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid tenant ID header"),
        @ApiResponse(responseCode = "500", description = "Internal server error during configuration deletion")
    })
    @DeleteMapping("/sample")
    public ResponseEntity<String> deleteTenantConfig(
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId) {

        log.info("Deleting tenant master list configuration for tenantId={}", tenantId);

        try {
            tenantMasterListConfigService.deleteTenantConfig(tenantId);
            log.info("Successfully deleted tenant configuration for tenantId={}", tenantId);
            return ResponseEntity.ok("Tenant configuration deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting tenant configuration: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /**
     * Creates a sample master list configuration for testing purposes.
     *
     * @return sample master list configuration
     */
    private Map<String, MasterListEntry> createSampleMasterListConfig() {
        Map<String, MasterListEntry> config = new HashMap<>();

        // Custom business name entry for this tenant
        config.put("MASTER_LABEL_BUSINESS_NAME", MasterListEntry.builder()
            .dataSource(MasterListDataSource.DB)
            .collection("notification_configurations")
            .path("configurationJson.businessName")
            .build());

        // Custom user name entry
        config.put("MASTER_LABEL_USER_NAME", MasterListEntry.builder()
            .dataSource(MasterListDataSource.PAYLOAD)
            .path("eventPayload.customerName")
            .build());

        // Custom OTP generator with different length
        Map<String, Object> otpConfig = new HashMap<>();
        otpConfig.put("length", 8);
        otpConfig.put("numeric", true);
        otpConfig.put("maxLength", 20);

        config.put("MASTER_LABEL_CUSTOM_OTP", MasterListEntry.builder()
            .dataSource(MasterListDataSource.GENERATE)
            .generator("OTP")
            .config(otpConfig)
            .build());

        // Custom tenant-specific label
        config.put("MASTER_LABEL_TENANT_CUSTOM", MasterListEntry.builder()
            .dataSource(MasterListDataSource.TOKEN)
            .path("claims.customField")
            .build());

        return config;
    }
}