package com.jio.partnerportal.controller;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.dto.request.DataBreachReportRequest;
import com.jio.partnerportal.dto.request.DataBreachUpdateRequest;
import com.jio.partnerportal.dto.response.DataBreachReportResponse;
import com.jio.partnerportal.dto.response.DataBreachReportSimpleResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.DataBreachReport;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.DataBreachReportService;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Validation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1.0/data-breach")
public class DataBreachReportController {

    private final DataBreachReportService service;
    private final Validation validation;

    @Autowired
    public DataBreachReportController(DataBreachReportService service, Validation validation) {
        this.service = service;
        this.validation = validation;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create")
    @Operation(
            summary = "Create a new Data Breach Report",
            description = "Creates a new data breach report with status NEW. Incident ID is auto-generated in the backend (format: DB-YYYY-XXX). Notification details are auto-generated with random data. tenant-id is required in headers.",
            parameters = {
                    @Parameter(name = "tenant-id", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DataBreachReportRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {
                                      "incidentDetails": {
                                        "discoveryDateTime": "2025-10-30T10:00:00Z",
                                        "occurrenceDateTime": "2025-10-29T22:00:00Z",
                                        "breachType": "UNAUTHORIZED_ACCESS",
                                        "briefDescription": "Email with user data sent to unintended recipient",
                                        "affectedSystemOrService": ["Consent API", "Consent API1"]
                                      },
                                      "dataInvolved": {
                                        "personalDataCategories": ["Name", "Contact Info", "Consent Logs"],
                                        "affectedDataPrincipalsCount": 500,
                                        "dataEncryptedOrProtected": true,
                                        "potentialImpactDescription": "Minor risk of unauthorized disclosure."
                                      }
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataBreachReportSimpleResponse.class)))
            }
    )
    public ResponseEntity<DataBreachReportSimpleResponse> create(@RequestHeader Map<String, String> headers,
                                                                  @RequestBody DataBreachReportRequest request, HttpServletRequest req) throws PartnerPortalException, BodyValidationException {
        validation.validateDataBreachReportRequest(request);
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        DataBreachReport saved = service.create(tenantId, request,req);
        return new ResponseEntity<>(service.mapToSimpleResponse(saved), HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/{id}")
    @Operation(
            summary = "Update a Data Breach Report",
            description = "Updates the status and remarks of a data breach report. Status can only progress sequentially (left to right). Only remarks and status can be updated.",
            parameters = {
                    @Parameter(name = "tenant-id", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "id", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DataBreachUpdateRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {
                                      "status": "INVESTIGATION",
                                      "remarks": "Forwarded to municipal officer"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataBreachReportSimpleResponse.class)))
            }
    )
    public ResponseEntity<DataBreachReportSimpleResponse> update(@RequestHeader Map<String, String> headers,
                                                                  @PathVariable("id") String id,
                                                                  @RequestBody DataBreachUpdateRequest request,HttpServletRequest req) throws PartnerPortalException, BodyValidationException {
        validation.validateDataBreachUpdateRequest(request);
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        DataBreachReport updated = service.update(tenantId, id, request,req);
        return new ResponseEntity<>(service.mapToSimpleResponse(updated), HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/{id}")
    @Operation(
            summary = "Get Data Breach Report by ID",
            description = "Retrieves a data breach report by its ID. tenant-id is required in headers.",
            parameters = {
                    @Parameter(name = "tenant-id", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "id", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataBreachReportResponse.class)))
            }
    )
    public ResponseEntity<DataBreachReportResponse> getById(@RequestHeader Map<String, String> headers,
                                                           @PathVariable("id") String id,HttpServletRequest req) throws PartnerPortalException {

        String activity = "Get Data Breach Report by ID";

        DataBreachReport report = service.getById(ThreadContext.get(Constants.TENANT_ID_HEADER), id);
        LogUtil.logActivity(req, activity, "Success: Get Data Breach Report by ID successfully");
        return new ResponseEntity<>(service.mapToResponse(report), HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping
    @Operation(
            summary = "Get all Data Breach Reports",
            description = "Retrieves all data breach reports for the tenant. tenant-id is required in headers.",
            parameters = {
                    @Parameter(name = "tenant-id", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SearchResponse.class)))
            }
    )
    public ResponseEntity<SearchResponse<DataBreachReportResponse>> getAll(@RequestHeader Map<String, String> headers,HttpServletRequest req) throws PartnerPortalException {
        String activity = "Get all Data Breach Reports";

        List<DataBreachReport> list = service.getAll(ThreadContext.get(Constants.TENANT_ID_HEADER));
        List<DataBreachReportResponse> mapped = list.stream().map(service::mapToResponse).toList();

        LogUtil.logActivity(req, activity, "Success: Get all Data Breach Reports successfully");
        return new ResponseEntity<>(SearchResponse.<DataBreachReportResponse>builder().searchList(mapped).build(), HttpStatus.OK);
    }
}
