package com.jio.digigov.fides.controller;

import com.jio.digigov.fides.constant.HeaderConstants;
import com.jio.digigov.fides.dto.request.DatasetRequest;
import com.jio.digigov.fides.dto.response.DatasetCountResponse;
import com.jio.digigov.fides.dto.response.DatasetListResponse;
import com.jio.digigov.fides.dto.response.DatasetResponse;
import com.jio.digigov.fides.dto.response.DatasetUpdateResponse;
import com.jio.digigov.fides.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
@Slf4j
public class DatasetController {

    private final DatasetService service;

    @PostMapping
    public ResponseEntity<DatasetResponse> create(
            @Valid @RequestBody DatasetRequest request,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId, HttpServletRequest req) {

        log.info("Creating dataset for tenantId: {}, businessId: {}", tenantId, businessId);

        return new ResponseEntity<>(service.create(request, tenantId, businessId, req), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<DatasetListResponse> list(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId) {

        log.info("Listing datasets for tenantId: {}, businessId: {}", tenantId, businessId);

        return new ResponseEntity<>(service.list(tenantId, businessId), HttpStatus.OK);
    }

    @GetMapping("/{datasetId}")
    public ResponseEntity<DatasetResponse> getById(
            @PathVariable String datasetId,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId) {

        log.info("Getting dataset by id: {} for tenantId: {}, businessId: {}", datasetId, tenantId, businessId);

        return new ResponseEntity<>(service.getById(datasetId, tenantId, businessId), HttpStatus.OK);
    }

    @PutMapping("/{datasetId}")
    public ResponseEntity<DatasetUpdateResponse> update(
            @PathVariable String datasetId,
            @Valid @RequestBody DatasetRequest request,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId, HttpServletRequest req) {

        log.info("Updating dataset id: {} for tenantId: {}, businessId: {}", datasetId, tenantId, businessId);

        return new ResponseEntity<>(service.update(datasetId, request, tenantId, businessId, req), HttpStatus.OK);
    }

    @DeleteMapping("/{datasetId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String datasetId,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId, HttpServletRequest req) {

        log.info("Deleting dataset id: {} for tenantId: {}, businessId: {}", datasetId, tenantId, businessId);

        service.delete(datasetId, tenantId, businessId, req);
        return ResponseEntity.ok(Map.of("message", "Dataset deleted successfully"));
    }

    @GetMapping("/count")
    @Operation(summary = "Get dataset Count")
    public ResponseEntity<DatasetCountResponse> count(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId) {

        log.info("Getting dataset count for tenantId: {}, businessId: {}", tenantId, businessId);

        return new ResponseEntity<>(service.getDatasetCounts(tenantId, businessId), HttpStatus.OK);
    }
}