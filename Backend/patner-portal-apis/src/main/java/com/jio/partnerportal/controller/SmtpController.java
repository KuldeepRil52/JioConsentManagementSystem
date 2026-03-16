package com.jio.partnerportal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.dto.SmtpDetails;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.ConfigCreateResponse;
import com.jio.partnerportal.dto.response.ConfigUpdateResponse;
import com.jio.partnerportal.dto.response.CountResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.dto.ErrorResponse;
import com.jio.partnerportal.entity.NotificationConfig;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.SmtpService;
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
@RequestMapping("/v1.0/smtp")
public class SmtpController {

    Validation validation;
    SmtpService smtpService;

    @Autowired
    public SmtpController(Validation validation, SmtpService smtpService) {
        this.validation = validation;
        this.smtpService = smtpService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create")
    @Operation(
            summary = "Create SMTP configuration",
            description = "Creates a new SMTP configuration.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
                    @Parameter(name = "business-id", description = "Business ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "scope-level", description = "Scope Level", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"TENANT", "BUSINESS"}, example = "TENANT")),
                    @Parameter(name = "provider-type", description = "Provider Type", required = false, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"DIGIGOV", "SMTP"}, example = "SMTP"))
            },
            requestBody = @RequestBody(
                    description = "Request body for creating an SMTP configuration",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfigurationRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "configurationJson": {
                                        "host": "smtp.example.com",
                                        "port": 587,
                                        "username": "user@example.com",
                                        "password": "yourpassword",
                                        "encryption": "TLS",
                                        "authentication": true,
                                        "defaultSenderEmail": "no-reply@example.com",
                                        "defaultSenderName": "Application Name"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "SMTP configuration created successfully",
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
    public ResponseEntity<ConfigCreateResponse> createConfig(@org.springframework.web.bind.annotation.RequestBody ConfigurationRequest<SmtpDetails> request, @RequestHeader Map<String, String> headers, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        this.validation.validateConfigHeader(headers);
        NotificationConfig config = this.smtpService.createConfig(headers, request, req);
        return new ResponseEntity<>(ConfigCreateResponse.builder().configId(config.getConfigId()).message("Config created successfully!").build(), HttpStatus.CREATED);
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/update/{configId}")
    @Operation(
            summary = "Update SMTP configuration",
            description = "Updates an existing SMTP configuration by its ID.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
                    @Parameter(name = "business-id", description = "Business ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "yourBusinessId")),
                    @Parameter(name = "scope-level", description = "Scope Level", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"GLOBAL", "BUSINESS", "INDIVIDUAL"}, example = "GLOBAL")),
                    @Parameter(name = "provider-type", description = "Provider Type", required = false, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"DIGIGOV", "SMTP"}, example = "SMTP")),
                    @Parameter(name = "configId", description = "ID of the SMTP configuration to update", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"))
            },
            requestBody = @RequestBody(
                    description = "Request body for updating an SMTP configuration",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfigurationRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "configurationJson": {
                                        "host": "smtp.updated.com",
                                        "port": 465,
                                        "username": "updateduser@example.com",
                                        "password": "newpassword",
                                        "encryption": "SSL",
                                        "authentication": false,
                                        "defaultSenderEmail": "info@example.com",
                                        "defaultSenderName": "New Application Name"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SMTP configuration updated successfully",
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
    public ResponseEntity<ConfigUpdateResponse> updateConfig(@org.springframework.web.bind.annotation.RequestBody ConfigurationRequest<SmtpDetails> request, @RequestHeader Map<String, String> headers, @PathVariable("configId") String configId, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        NotificationConfig config = this.smtpService.updateConfig(configId, request, headers, req);
        return new ResponseEntity<>(ConfigUpdateResponse.builder().configId(config.getConfigId()).message("Config updated successfully!").build(), HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/search")
    @Operation(
            summary = "Search SMTP configurations",
            description = "Searches for SMTP configurations based on provided criteria like business ID, scope level, and config ID.",
            parameters = {
                    @Parameter(name = "businessId", description = "Business ID associated with the SMTP configuration", in = ParameterIn.QUERY, example = "yourBusinessId"),
                    @Parameter(name = "scopeLevel", description = "Scope Level of the SMTP configuration", in = ParameterIn.QUERY, schema = @Schema(type = "string", allowableValues = {"GLOBAL", "BUSINESS", "INDIVIDUAL"}), example = "GLOBAL"),
                    @Parameter(name = "configId", description = "ID of the SMTP configuration", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
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
    public ResponseEntity<SearchResponse<NotificationConfig>> search(@Parameter(hidden = true) @RequestParam Map<String, String> reqParams, HttpServletRequest req) throws Exception {
        SearchResponse<NotificationConfig> response = this.smtpService.search(reqParams, req);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/count")
    @Operation(
            summary = "Count SMTP configurations",
            description = "Returns the total count of SMTP configurations.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
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
    public ResponseEntity<CountResponse> count(HttpServletRequest req) throws PartnerPortalException, BodyValidationException {
        String activity = "Count SMTP configurations";

        LogUtil.logActivity(req, activity, "Success: Count SMTP configurations successfully");
        return new ResponseEntity<>(CountResponse.builder().count(this.smtpService.count()).build(), HttpStatus.OK);
    }
}
