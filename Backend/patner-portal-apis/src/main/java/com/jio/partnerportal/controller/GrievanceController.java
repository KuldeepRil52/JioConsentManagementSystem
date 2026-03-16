package com.jio.partnerportal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.dto.GrievanceDetails;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.ConfigCreateResponse;
import com.jio.partnerportal.dto.response.ConfigUpdateResponse;
import com.jio.partnerportal.dto.response.CountResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.dto.ErrorResponse;
import com.jio.partnerportal.entity.GrievanceConfig;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.GrievanceService;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Validation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

@RestController
@RequestMapping("/v1.0/grievance")
public class GrievanceController {

    Validation validation;
    GrievanceService grievanceService;

    @Autowired
    public GrievanceController(Validation validation, GrievanceService grievanceService) {
        this.validation = validation;
        this.grievanceService = grievanceService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create")
    @Operation(
            summary = "Create grievance configuration",
            description = "Creates a new grievance configuration.",
            parameters = {
                    @Parameter(name = "business-id", description = "Business ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "scope-level", description = "Scope Level", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"TENANT", "BUSINESS"}, example = "TENANT")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            requestBody = @RequestBody(
                    description = "Request body for creating a grievance configuration",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfigurationRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "configurationJson": {
                                        "grievanceTypes": [
                                          "ACCESS",
                                          "CORRECTION"
                                        ],
                                        "endpointUrl": "https://privacy.example.com/grievance",
                                        "intakeMethods": [
                                          "WEB_FORM",
                                          "MOBILE_APP"
                                        ],
                                        "workflow": [
                                          "New",
                                          "In Progress",
                                          "Escalated",
                                          "Resolved"
                                        ],
                                        "slaTimeline": {
                                          "value": 6,
                                          "unit": "MONTHS"
                                        },
                                        "escalationPolicy": {
                                          "value": 3,
                                          "unit": "MONTHS"
                                        },
                                        "retentionPolicy": {
                                          "value": 1,
                                          "unit": "YEARS"
                                        },
                                        "communicationConfig": {
                                          "sms": true,
                                          "email": true
                                        }
                                      }
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Grievance configuration created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigCreateResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "parameter": "Optional: This field will be present if error caused by request parameters.",
                                                  "header": "Optional: This field will be present if error caused by request header.",
                                                  "errorMessage": "Validation failed: companyName cannot be null",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Not Found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Not Found: Data could not be found.",
                                                  "errorCode": "JCMP3001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Internal Server Error",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    )
            }
    )
    public ResponseEntity<ConfigCreateResponse> createConfig(@org.springframework.web.bind.annotation.RequestBody ConfigurationRequest<GrievanceDetails> request, @RequestHeader Map<String, String> headers, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        this.validation.validateConfigHeader(headers);
        GrievanceConfig config = this.grievanceService.createConfig(headers, request, req);
        return new ResponseEntity<>(ConfigCreateResponse.builder().configId(config.getConfigId()).message("Config created successfully!").build(), HttpStatus.CREATED);
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/update/{configId}")
    @Operation(
            summary = "Update grievance configuration",
            description = "Updates an existing grievance configuration by its ID.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
                    @Parameter(name = "configId", description = "ID of the grievance configuration to update", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"))
            },
            requestBody = @RequestBody(
                    description = "Request body for updating a grievance configuration",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfigurationRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "configurationJson": {
                                        "grievanceTypes": [
                                          "ACCESS",
                                          "CORRECTION"
                                        ],
                                        "endpointUrl": "https://privacy.example.com/grievance-updated",
                                        "intakeMethods": [
                                          "WEB_FORM",
                                          "EMAIL"
                                        ],
                                        "workflow": [
                                          "New",
                                          "In Progress",
                                          "Resolved"
                                        ],
                                        "slaTimeline": {
                                          "value": 1,
                                          "unit": "YEARS"
                                        },
                                        "escalationPolicy": {
                                          "value": 6,
                                          "unit": "MONTHS"
                                        },
                                        "retentionPolicy": {
                                          "value": 5,
                                          "unit": "YEARS"
                                        },
                                        "communicationConfig": {
                                          "sms": true,
                                          "email": true
                                        }
                                      }
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Grievance configuration updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigUpdateResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "parameter": "Optional: This field will be present if error caused by request parameters.",
                                                  "header": "Optional: This field will be present if error caused by request header.",
                                                  "errorMessage": "Validation failed: companyName cannot be null",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Not Found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Not Found: Data could not be found.",
                                                  "errorCode": "JCMP3001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Internal Server Error",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    )
            }
    )
    public ResponseEntity<ConfigUpdateResponse> updateConfig(@org.springframework.web.bind.annotation.RequestBody ConfigurationRequest<GrievanceDetails> request, @RequestHeader Map<String, String> headers, @PathVariable("configId") String configId, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        GrievanceConfig config = this.grievanceService.updateConfig(configId, request, req);
        return new ResponseEntity<>(ConfigUpdateResponse.builder().configId(config.getConfigId()).message("Config updated successfully!").build(), HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/search")
    @Operation(
            summary = "Search grievance configurations",
            description = "Searches for grievance configurations based on provided criteria like business ID, scope level, and config ID.",
            parameters = {
                    @Parameter(name = "businessId", description = "ID of the business to search for", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "scopeLevel", description = "Scope level of the grievance configuration", in = ParameterIn.QUERY, example = "TENANT", schema = @Schema(type = "string", allowableValues = {"TENANT", "BUSINESS"})),
                    @Parameter(name = "configId", description = "ID of the grievance configuration", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Search successful",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SearchResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "parameter": "Optional: This field will be present if error caused by request parameters.",
                                                  "header": "Optional: This field will be present if error caused by request header.",
                                                  "errorMessage": "Validation failed: companyName cannot be null",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Not Found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Not Found: Data could not be found.",
                                                  "errorCode": "JCMP3001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Internal Server Error",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    )
            }
    )
    public ResponseEntity<SearchResponse<GrievanceConfig>> search(@Parameter(hidden = true) @RequestParam Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        SearchResponse<GrievanceConfig> response = this.grievanceService.search(reqParams, req);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/count")
    @Operation(
            summary = "Count grievance configurations",
            description = "Returns the total count of grievance configurations.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Count successful",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "parameter": "Optional: This field will be present if error caused by request parameters.",
                                                  "header": "Optional: This field will be present if error caused by request header.",
                                                  "errorMessage": "Validation failed: companyName cannot be null",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Not Found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Not Found: Data could not be found.",
                                                  "errorCode": "JCMP3001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Internal Server Error",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    )
            }
    )
    public ResponseEntity<CountResponse> count(HttpServletRequest req) {
        String activity = "Count grievance configurations ";

        LogUtil.logActivity(req, activity, "Success: Count grievance configurations successfully");
        return new ResponseEntity<>(CountResponse.builder().count(this.grievanceService.count()).build(), HttpStatus.OK);
    }

}
