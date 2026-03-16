package com.jio.digigov.notification.controller.v1.masterlist;

import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.masterlist.AddMasterLabelRequestDto;
import com.jio.digigov.notification.dto.request.masterlist.UpdateMasterLabelRequestDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing master labels within tenant master list configurations.
 *
 * This controller provides APIs to create, read, update, and delete master labels
 * that are used for data resolution in notification templates.
 */
@RestController
@RequestMapping("/v1/master-lists/labels")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Master Label Management", description = "APIs for managing master labels in configurations")
public class MasterLabelController extends BaseController {

    private final MasterLabelService masterLabelService;

    @Operation(summary = "Add new master label",
               description = "Creates a new master label with event associations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Label created successfully"),
        @ApiResponse(responseCode = "409", description = "Label already exists")
    })
    @PostMapping
    public ResponseEntity<StandardApiResponseDto<Void>> addMasterLabel(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Master label creation request", required = true)
            @Valid @RequestBody AddMasterLabelRequestDto request) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            masterLabelService.addMasterLabel(tenantId, request.getLabelName(),
                                            request.getEntry(), request.getEvents());

            StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>success(
                String.format("Master label '%s' added successfully", request.getLabelName())
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (TenantMasterListConfigService.TenantMasterListConfigException e) {
            if (e.getMessage().contains("already exists")) {
                StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>error(
                    "JDNM4003",
                    String.format("Master label '%s' already exists", request.getLabelName())
                ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());

                return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResponse);
            }
            log.error("Error adding master label for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>validationError(e.getMessage())
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Error adding master label for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Update master label",
               description = "Updates configuration for a specific master label")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Label updated successfully"),
        @ApiResponse(responseCode = "404", description = "Label not found")
    })
    @PutMapping("/{labelName}")
    public ResponseEntity<StandardApiResponseDto<Void>> updateMasterLabel(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String labelName,
            @Valid @RequestBody UpdateMasterLabelRequestDto request) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            masterLabelService.updateMasterLabel(tenantId, labelName,
                                               request.getEntry(), request.getEvents());

            StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>success(
                String.format("Master label '%s' updated successfully", labelName)
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (TenantMasterListConfigService.TenantMasterListConfigException e) {
            if (e.getMessage().contains("not found")) {
                StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>notFound(
                    String.format("Master label '%s'", labelName)
                ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
            }
            log.error("Error updating master label for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>validationError(e.getMessage())
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Error updating master label for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Delete master label",
               description = "Removes a master label from the configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Label deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Label not found")
    })
    @DeleteMapping("/{labelName}")
    public ResponseEntity<StandardApiResponseDto<Void>> deleteMasterLabel(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String labelName) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            masterLabelService.deleteMasterLabel(tenantId, labelName);

            StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>success(
                String.format("Master label '%s' deleted successfully", labelName)
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (TenantMasterListConfigService.TenantMasterListConfigException e) {
            if (e.getMessage().contains("not found")) {
                StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>notFound(
                    String.format("Master label '%s'", labelName)
                ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
            }
            log.error("Error deleting master label for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>validationError(e.getMessage())
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Error deleting master label for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Get master label details",
               description = "Retrieves detailed information about a specific master label")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Label details retrieved"),
        @ApiResponse(responseCode = "404", description = "Label not found")
    })
    @GetMapping("/{labelName}")
    public ResponseEntity<StandardApiResponseDto<MasterLabelService.MasterLabelDetails>> getLabelDetails(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String labelName,
            @RequestParam(defaultValue = "false") boolean includeEventMappings) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            MasterLabelService.MasterLabelDetails labelDetails = masterLabelService.getLabelDetails(
                tenantId, labelName, includeEventMappings);

            StandardApiResponseDto<MasterLabelService.MasterLabelDetails> apiResponse = StandardApiResponseDto.success(
                labelDetails,
                String.format("Retrieved details for master label '%s'", labelName)
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (TenantMasterListConfigService.TenantMasterListConfigException e) {
            if (e.getMessage().contains("not found")) {
                StandardApiResponseDto<MasterLabelService.MasterLabelDetails> apiResponse =
                        StandardApiResponseDto.<MasterLabelService.MasterLabelDetails>notFound(
                    String.format("Master label '%s'", labelName)
                ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
            }
            log.error("Error getting label details for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<MasterLabelService.MasterLabelDetails> apiResponse =
                    StandardApiResponseDto.<MasterLabelService.MasterLabelDetails>validationError(e.getMessage())
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Error getting label details for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<MasterLabelService.MasterLabelDetails> apiResponse =
                    StandardApiResponseDto.<MasterLabelService.MasterLabelDetails>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }
}