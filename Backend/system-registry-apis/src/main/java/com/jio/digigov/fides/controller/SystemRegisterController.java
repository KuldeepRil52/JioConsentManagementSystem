package com.jio.digigov.fides.controller;

import com.jio.digigov.fides.constant.HeaderConstants;
import com.jio.digigov.fides.dto.request.SystemRegisterRequest;
import com.jio.digigov.fides.dto.response.SystemCountResponse;
import com.jio.digigov.fides.dto.response.SystemRegisterListResponse;
import com.jio.digigov.fides.dto.response.SystemRegisterResponse;
import com.jio.digigov.fides.dto.response.SystemRegisterUpdateResponse;
import com.jio.digigov.fides.service.SystemRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/systems")
@RequiredArgsConstructor
@Tag(name = "System Register")
@Slf4j
public class SystemRegisterController {

    private final SystemRegisterService service;

    @PostMapping
    public ResponseEntity<SystemRegisterResponse> create(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @Valid @RequestBody SystemRegisterRequest request, HttpServletRequest req) {

        log.info("Creating system register for tenantId: {}, businessId: {}", tenantId, businessId);

        return new ResponseEntity<>(service.create(tenantId, businessId, request, req), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<SystemRegisterListResponse> list(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId) {

        log.info("Listing system registers for tenantId: {}, businessId: {}", tenantId, businessId);

        return new ResponseEntity<>(service.findAll(tenantId, businessId), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SystemRegisterResponse> getById(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @PathVariable String id) {

        log.info("Getting system register by id: {} for tenantId: {}, businessId: {}", id, tenantId, businessId);

        return new ResponseEntity<>(service.findById(tenantId, businessId, id), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SystemRegisterUpdateResponse> update(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @PathVariable String id,
             @Valid @RequestBody SystemRegisterRequest request, HttpServletRequest req) {

        log.info("Updating system register id: {} for tenantId: {}, businessId: {}", id, tenantId, businessId);

        return new ResponseEntity<>(service.update(tenantId, businessId, id, request, req), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @PathVariable String id, HttpServletRequest req) {

        log.info("Deleting system register id: {} for tenantId: {}, businessId: {}", id, tenantId, businessId);

        service.delete(tenantId, businessId, id, req);
        return ResponseEntity.ok(Map.of("message", "System deleted successfully"));
    }

    @GetMapping("/count")
    @Operation(summary = "Get System Count")
    public ResponseEntity<SystemCountResponse> count(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId) {

        log.info("Getting system count for tenantId: {}, businessId: {}", tenantId, businessId);

        return new ResponseEntity<>(service.getSystemCounts(tenantId, businessId), HttpStatus.OK);
    }
}