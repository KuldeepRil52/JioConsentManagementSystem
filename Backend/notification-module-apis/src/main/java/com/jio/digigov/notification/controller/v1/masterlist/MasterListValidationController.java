package com.jio.digigov.notification.controller.v1.masterlist;

import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.masterlist.MasterListValidationRequestDto;
import com.jio.digigov.notification.dto.request.masterlist.ResolutionTestRequestDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.masterlist.ValidationResponseDto;
import com.jio.digigov.notification.enums.EventType;
import com.jio.digigov.notification.service.masterlist.MasterLabelService;
import com.jio.digigov.notification.service.masterlist.TenantMasterListConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for validation and testing of master list configurations.
 *
 * This controller provides APIs to validate master list configurations,
 * test label resolution, and verify data integrity within tenant configurations.
 */
@RestController
@RequestMapping("/v1/master-lists/validation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Master List Validation", description = "APIs for validating and testing master list configurations")
public class MasterListValidationController extends BaseController {

    private final MasterLabelService masterLabelService;

    @Operation(summary = "Validate master labels for event",
               description = "Validates that specified labels exist and are properly configured for an event type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed"),
        @ApiResponse(responseCode = "400", description = "Validation errors found")
    })
    @PostMapping("/events/{eventType}/labels")
    public ResponseEntity<StandardApiResponseDto<ValidationResponseDto>> validateLabelsForEvent(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Event type to validate against", required = true)
            @PathVariable EventType eventType,
            @Valid @RequestBody MasterListValidationRequestDto request) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            ValidationResponseDto validationResult = masterLabelService.validateLabelsForEvent(
                tenantId, eventType, request.getMasterLabels());

            StandardApiResponseDto<ValidationResponseDto> apiResponse = StandardApiResponseDto.success(
                validationResult,
                String.format("Validation completed for event type %s", eventType)
            ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (TenantMasterListConfigService.TenantMasterListConfigException e) {
            log.error("Validation error for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<ValidationResponseDto> apiResponse = StandardApiResponseDto
                .<ValidationResponseDto>validationError(e.getMessage())
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Error validating labels for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<ValidationResponseDto> apiResponse = StandardApiResponseDto
                .<ValidationResponseDto>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Test master label resolution",
            description = "Tests resolution of master labels with provided sample data to " +
                    "verify configuration correctness")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resolution test completed"),
        @ApiResponse(responseCode = "400", description = "Test parameters invalid or resolution failed")
    })
    @PostMapping("/{labelName}/resolution-test")
    public ResponseEntity<StandardApiResponseDto<MasterLabelService.ResolutionTestResult>> testLabelResolution(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String labelName,
            @Valid @RequestBody ResolutionTestRequestDto request) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            MasterLabelService.ResolutionTestResult testResult = masterLabelService.testLabelResolution(
                tenantId, labelName, request.getEventPayload(), request.getEventType());

            StandardApiResponseDto<MasterLabelService.ResolutionTestResult> apiResponse = StandardApiResponseDto
                .success(testResult, "Label resolution test completed successfully")
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (TenantMasterListConfigService.TenantMasterListConfigException e) {
            log.error("Resolution test error for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<MasterLabelService.ResolutionTestResult> apiResponse = StandardApiResponseDto
                .<MasterLabelService.ResolutionTestResult>validationError(e.getMessage())
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Error testing label resolution for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<MasterLabelService.ResolutionTestResult> apiResponse = StandardApiResponseDto
                .<MasterLabelService.ResolutionTestResult>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }
}