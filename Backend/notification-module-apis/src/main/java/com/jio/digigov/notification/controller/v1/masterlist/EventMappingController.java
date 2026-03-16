package com.jio.digigov.notification.controller.v1.masterlist;

import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.masterlist.AddEventMappingRequestDto;
import com.jio.digigov.notification.dto.request.masterlist.UpdateEventMappingRequestDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.masterlist.EventTypeInfo;
import com.jio.digigov.notification.dto.response.masterlist.MasterListEventResponseDto;
import com.jio.digigov.notification.enums.EventType;
import com.jio.digigov.notification.service.masterlist.EventMappingService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for managing event mappings in master list configurations.
 *
 * This controller provides APIs to manage the relationship between event types
 * and master labels within tenant-specific master list configurations.
 */
@RestController
@RequestMapping("/v1/master-lists/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event Mapping Management", description = "APIs for managing event to master label mappings")
public class EventMappingController extends BaseController {

    private final EventMappingService eventMappingService;

    @Operation(summary = "Get master lists for event type",
               description = "Returns all master labels available for a specific event type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Master labels retrieved"),
        @ApiResponse(responseCode = "404", description = "Event type not configured")
    })
    @GetMapping("/{eventType}")
    public ResponseEntity<StandardApiResponseDto<MasterListEventResponseDto>> getMasterListsForEvent(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Event type identifier", required = true, example = "CONSENT_GRANTED")
            @PathVariable EventType eventType) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            // TODO: Implementation would go here
            MasterListEventResponseDto responseData = null; // Placeholder

            StandardApiResponseDto<MasterListEventResponseDto> apiResponse = StandardApiResponseDto.success(
                responseData,
                "Master lists retrieved for event type"
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error retrieving master lists for event {} for tenant {}: {}",
                     eventType, tenantId, e.getMessage(), e);

            StandardApiResponseDto<MasterListEventResponseDto> apiResponse =
                    StandardApiResponseDto.<MasterListEventResponseDto>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Add master labels to event type",
               description = "Associates master labels with an event type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Mappings added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid labels or event type")
    })
    @PostMapping("/{eventType}/mappings")
    public ResponseEntity<StandardApiResponseDto<Void>> addEventMapping(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Event type identifier", required = true, example = "CONSENT_GRANTED")
            @PathVariable EventType eventType,
            @Parameter(description = "Event mapping request with master labels", required = true)
            @Valid @RequestBody AddEventMappingRequestDto request) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            eventMappingService.addEventMapping(tenantId, eventType, request.getMasterLabels());

            StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>success(
                String.format("Event mapping added successfully for %s", eventType)
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (TenantMasterListConfigService.TenantMasterListConfigException e) {
            log.error("Error adding event mapping for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>validationError(e.getMessage())
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Error adding event mapping for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Remove event mappings",
               description = "Removes all master label associations for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Mappings removed successfully"),
        @ApiResponse(responseCode = "404", description = "Event type not found")
    })
    @DeleteMapping("/{eventType}/mappings")
    public ResponseEntity<StandardApiResponseDto<Void>> removeEventMapping(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Event type identifier", required = true, example = "CONSENT_GRANTED")
            @PathVariable EventType eventType) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            eventMappingService.removeEventMapping(tenantId, eventType);

            StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>success(
                String.format("Event mapping removed successfully for %s", eventType)
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error removing event mapping for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Update event mappings",
               description = "Replaces all master label associations for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mappings updated successfully")
    })
    @PutMapping("/{eventType}/mappings")
    public ResponseEntity<StandardApiResponseDto<Void>> updateEventMapping(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Event type identifier", required = true, example = "CONSENT_GRANTED")
            @PathVariable EventType eventType,
            @Parameter(description = "Updated event mapping request with master labels", required = true)
            @Valid @RequestBody UpdateEventMappingRequestDto request) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            eventMappingService.updateEventMapping(tenantId, eventType, request.getMasterLabels());

            StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>success(
                String.format("Event mapping updated successfully for %s", eventType)
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error updating event mapping for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Get all supported event types",
               description = "Returns all event types with their descriptions and current configurations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event types retrieved")
    })
    @GetMapping
    public ResponseEntity<StandardApiResponseDto<List<EventTypeInfo>>> getAllEventTypes(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {

        String correlationId = extractCorrelationId(httpRequest);

        try {
            List<EventTypeInfo> eventTypes = java.util.Arrays.stream(EventType.values())
                .map(eventType -> {
                    String category = getEventCategory(eventType);
                    // Check if event is configured (simplified)
                    boolean isConfigured = false;
                    int labelCount = 0;

                    try {
                        var labels = eventMappingService.getLabelsForEvent(tenantId, eventType);
                        isConfigured = !labels.isEmpty();
                        labelCount = labels.size();
                    } catch (Exception e) {
                        // Event not configured or no active config
                    }

                    return EventTypeInfo.builder()
                        .eventType(eventType)
                        .description(eventType.getDescription())
                        .category(category)
                        .isConfigured(isConfigured)
                        .labelCount(labelCount)
                        .build();
                })
                .collect(Collectors.toList());

            StandardApiResponseDto<List<EventTypeInfo>> apiResponse = StandardApiResponseDto.success(
                eventTypes,
                String.format("Retrieved %d event types", eventTypes.size())
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error getting event types for tenant {}: {}", tenantId, e.getMessage());

            StandardApiResponseDto<List<EventTypeInfo>> apiResponse =
                    StandardApiResponseDto.<List<EventTypeInfo>>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    private String getEventCategory(EventType eventType) {
        String name = eventType.name();
        if (name.startsWith("CONSENT_")) return "CONSENT";
        if (name.startsWith("GRIEVANCE_")) return "GRIEVANCE";
        if (name.startsWith("DATA_")) return "DATA";
        return "OTHER";
    }
}