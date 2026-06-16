package com.jio.digigov.fides.controller;

import com.jio.digigov.fides.constant.HeaderConstants;
import com.jio.digigov.fides.dto.request.DbIntegrationCreateRequest;
import com.jio.digigov.fides.dto.request.DbIntegrationTestRequest;
import com.jio.digigov.fides.dto.request.DbIntegrationUpdateRequest;
import com.jio.digigov.fides.dto.response.DbIntegrationResponse;
import com.jio.digigov.fides.entity.DbIntegration;
import com.jio.digigov.fides.service.DbIntegrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/db-integration")
@RequiredArgsConstructor
@Tag(name = "DB Integration")
public class DbIntegrationController {

    private final DbIntegrationService service;

    @GetMapping("/db-types")
    public ResponseEntity<Map<String, Object>> dbTypes() {
        return ResponseEntity.ok(service.getSupportedDbTypes());
    }

    @PostMapping
    public ResponseEntity<DbIntegrationResponse> create(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @Valid @RequestBody DbIntegrationCreateRequest request,
            HttpServletRequest req
    ) {
        return ResponseEntity.ok(service.create(tenantId, businessId, request, req));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestParam(required = false) String systemId
    ) {
        List<DbIntegration> list = service.list(tenantId, businessId, systemId);
        return ResponseEntity.ok(Map.of(
                "totalRecords", list.size(),
                "integrations", list
        ));
    }

    @GetMapping("/{integrationId}")
    public ResponseEntity<DbIntegration> get(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @PathVariable String integrationId
    ) {
        return ResponseEntity.ok(
                service.getByIntegrationId(tenantId, businessId, integrationId)
        );
    }

    @PutMapping("/{integrationId}")
    public ResponseEntity<DbIntegrationResponse> update(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @PathVariable String integrationId,
            @Valid @RequestBody DbIntegrationUpdateRequest request,
            HttpServletRequest req
    ) {
        return ResponseEntity.ok(
                service.update(tenantId, businessId, integrationId, request, req)
        );
    }

    @DeleteMapping("/{integrationId}")
    public ResponseEntity<Map<String, String>> delete(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @PathVariable String integrationId,
            HttpServletRequest req
    ) {
        service.delete(tenantId, businessId, integrationId, req);
        return ResponseEntity.ok(Map.of("message", "Integration deleted successfully"));
    }

    @PostMapping("/map-dataset")
    public ResponseEntity<Map<String, Object>> mapDataset(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestBody Map<String, String> request, HttpServletRequest req
    ) {
        DbIntegration integration = service.mapDataset(
                tenantId,
                businessId,
                request.get("integrationId"),
                request.get("datasetId"),
                req
        );

        return ResponseEntity.ok(Map.of(
                "message", "Integration mapped to dataset successfully",
                "integrationId", integration.getIntegrationId(),
                "datasetId", integration.getDatasetId()
        ));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test(
            @RequestBody DbIntegrationTestRequest request
    ) {
        return ResponseEntity.ok(service.testConnection(request));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> count(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId
    ) {
        return ResponseEntity.ok(service.count(tenantId, businessId));
    }
}