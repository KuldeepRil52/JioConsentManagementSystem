package com.jio.partnerportal.controller;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.dto.response.DataBreachNotifyResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.DataBreachNotify;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.DataBreachNotifierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Controller to trigger data-breach -> consent correlation and notification persistence.
 *
 * POST /v1.0/data-breach/notify/{incidentId}
 *
 * Headers:
 *  - tenant-id (required)
 *  - txn (required)
 *  - x-session-token (required)
 *
 * Response: saved DataBreachNotifyResponse document (with consentIds and customer identifiers)
 */
@RestController
@RequestMapping("/v1.0/data-breach")
public class DataBreachNotifierController {

    private final DataBreachNotifierService notifierService;

    public DataBreachNotifierController(DataBreachNotifierService notifierService) {
        this.notifierService = notifierService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/notify/{incidentId}")
    @Operation(
            summary = "Notify consents impacted by data-breach incident",
            description = "Finds personalDataCategories for the given incident in tenant DB and searches tenant 'consents' collection for consents that reference any of the categories. Persists a data_breach_notify record and returns it.",
            parameters = {
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "incidentId", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "x-session-token", description = "Session token", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notifications processed and saved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataBreachNotify.class))),
                    @ApiResponse(responseCode = "404", description = "TenantId, incidentId or personalDataCategories not found"),
                    @ApiResponse(responseCode = "500", description = "Data breach notification already in progress or completed for this incident.")
            }
    )
    public ResponseEntity<DataBreachNotifyResponse> notifyByIncident(@PathVariable("incidentId") String incidentId,
                                                                     @RequestHeader Map<String, String> headers) throws PartnerPortalException {
        // tenant-id is stored in threadcontext by AuthUtility in many flows; guard: if not present try headers
        String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = headers.get(Constants.TENANT_ID_HEADER);
        }
        DataBreachNotifyResponse saved = notifierService.processAndNotify(tenantId, incidentId);
        return new ResponseEntity<>(saved, HttpStatus.OK);
    }
}