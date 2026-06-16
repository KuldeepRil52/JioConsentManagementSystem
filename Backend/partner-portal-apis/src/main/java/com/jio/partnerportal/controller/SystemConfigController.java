package com.jio.partnerportal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.dto.SystemDetails;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.ConfigCreateResponse;
import com.jio.partnerportal.dto.response.ConfigUpdateResponse;
import com.jio.partnerportal.dto.response.CountResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.dto.ErrorResponse;
import com.jio.partnerportal.entity.SystemConfig;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.SystemConfigService;
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
@RequestMapping("/v1.0/system-config")
public class SystemConfigController {

    Validation validation;
    SystemConfigService systemConfigService;

    @Autowired
    public SystemConfigController(Validation validation, SystemConfigService systemConfigService) {
        this.validation = validation;
        this.systemConfigService = systemConfigService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create")
    @Operation(
            summary = "Create system configuration",
            description = "Creates a new system configuration.",
            parameters = {
                    @Parameter(name = "business-id", description = "Business ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "scope-level", description = "Scope Level", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"BUSINESS", "TENANT"}, example = "TENANT")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            requestBody = @RequestBody(
                    description = "Request body for creating a system configuration",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfigurationRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "configurationJson": {
                                    "sslCertificate": "-----BEGIN CERTIFICATE-----\\nMIIDdzCCAl+gAwIBAgIJAN2g/0c...\\n-----END CERTIFICATE-----",
                                    "sslCertificateMeta": {
                                      "name": "ssl-certificate.pem",
                                      "contentType": "application/x-pem-file",
                                      "size": 1024,
                                      "tag": {
                                        "tagName": "SSL_CERTIFICATE",
                                        "tagValue": "system-ssl-cert"
                                      }
                                    },
                                    "logo": "iVBORw0KGgoAAAANSUhEUgAAAAUA...",
                                    "logoMeta": {
                                      "name": "company-logo.png",
                                      "contentType": "image/png",
                                      "size": 2048,
                                      "tag": {
                                        "tagName": "LOGO",
                                        "tagValue": "company-logo"
                                      }
                                    },
                                    "baseUrl": "https://api.yourcompany.com/v1",
                                    "defaultConsentExpiryDays": 365,
                                    "jwtTokenTTLMinutes": 60,
                                    "signedArtifactExpiryDays": 30,
                                    "dataRetention": {
                                                      "value": 5,
                                                      "unit": "YEARS"
                                                    },
                                    "clientId": "your-client-id-here",
                                    "clientSecret": "your-client-secret-here",
                                    "keystoreData": "MIIKpAIBAzCCCl4GCSqGSIb3DQEHBqCCCl8wggpbAgEAMIIKUQYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQI...",
                                    "keystorePassword": "password123",
                                    "alias": "form65-cert",
                                    "keystoreName": "abc.p12"
                                  }
                                }
                                """
                            )

                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "System configuration created successfully",
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
    public ResponseEntity<ConfigCreateResponse> createConfig(@org.springframework.web.bind.annotation.RequestBody ConfigurationRequest<SystemDetails> request, @RequestHeader Map<String, String> headers, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        this.validation.validateConfigHeader(headers);
        SystemConfig config = this.systemConfigService.createConfig(headers, request, req);
        return new ResponseEntity<>(ConfigCreateResponse.builder().configId(config.getConfigId()).message("Config created successfully!").build(), HttpStatus.CREATED);
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/update/{configId}")
    @Operation(
            summary = "Update system configuration",
            description = "Updates an existing system configuration by its ID.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
                    @Parameter(name = "is-ssl-certificate-modified", description = "Flag to indicate if SSL certificate has been modified", required = false, in = ParameterIn.HEADER, schema = @Schema(type = "boolean", example = "false")),
                    @Parameter(name = "is-logo-modified", description = "Flag to indicate if logo has been modified", required = false, in = ParameterIn.HEADER, schema = @Schema(type = "boolean", example = "false")),
                    @Parameter(name = "configId", description = "ID of the system configuration to update", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"))
            },
            requestBody = @RequestBody(
                    description = "Request body for updating a system configuration",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfigurationRequest.class),
                            examples = @ExampleObject(value = """
                            {
                              "configurationJson": {
                                "sslCertificate": "-----BEGIN CERTIFICATE-----\\nMIIDdzCCAl+gAwIBAgIJAN2g/0c...\\n-----END CERTIFICATE-----",
                                "sslCertificateMeta": {
                                  "documentId": "existing-doc-id-123",
                                  "name": "updated-ssl-certificate.pem",
                                  "contentType": "application/x-pem-file",
                                  "size": 1024,
                                  "tag": {
                                    "tagName": "SSL_CERTIFICATE",
                                    "tagValue": "system-ssl-cert"
                                  }
                                },
                                "logo": "iVBORw0KGgoAAAANSUhEUgAAAAUA...",
                                "logoMeta": {
                                  "documentId": "existing-logo-id-456",
                                  "name": "updated-company-logo.png",
                                  "contentType": "image/png",
                                  "size": 2048,
                                  "tag": {
                                    "tagName": "LOGO",
                                    "tagValue": "company-logo"
                                  }
                                },
                                "baseUrl": "https://api.yourcompany.com/v1",
                                "defaultConsentExpiryDays": 365,
                                "jwtTokenTTLMinutes": 60,
                                "signedArtifactExpiryDays": 30,
                                "dataRetention": {
                                                      "value": 5,
                                                      "unit": "YEARS"
                                                    },
                                "clientId": "your-client-id-here",
                                "clientSecret": "your-client-secret-here",
                                "keystoreData": "MIIKpAIBAzCCCl4GCSqGSIb3DQEHBqCCCl8wggpbAgEAMIIKUQYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQI...",
                                "keystorePassword": "password123",
                                "alias": "form65-cert",
                                "keystoreName": "abc.p12"
                              }
                            }
                            """)

                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "System configuration updated successfully",
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
    public ResponseEntity<ConfigUpdateResponse> updateConfig(
            @org.springframework.web.bind.annotation.RequestBody ConfigurationRequest<SystemDetails> request, 
            @RequestHeader Map<String, String> headers, 
            @PathVariable("configId") String configId,
            @RequestHeader(value = "is-ssl-certificate-modified", required = false, defaultValue = "false") boolean isSslCertificateModified,
            @RequestHeader(value = "is-logo-modified", required = false, defaultValue = "false") boolean isLogoModified, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        SystemConfig config = this.systemConfigService.updateConfig(configId, request, isSslCertificateModified, isLogoModified, req);
        return new ResponseEntity<>(ConfigUpdateResponse.builder().configId(config.getConfigId()).message("Config updated successfully!").build(), HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/search")
    @Operation(
            summary = "Search system configurations",
            description = "Searches for system configurations based on provided criteria like business ID, scope level, and config ID.",
            parameters = {
                    @Parameter(name = "businessId", description = "ID of the business to search for", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "scopeLevel", description = "Scope level of the system configuration", in = ParameterIn.QUERY, example = "TENANT", schema = @Schema(type = "string", allowableValues = {"TENANT", "BUSINESS"})),
                    @Parameter(name = "configId", description = "ID of the system configuration", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
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
    public ResponseEntity<SearchResponse<SystemConfig>> search(@Parameter(hidden = true) @RequestParam Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        SearchResponse<SystemConfig> response = this.systemConfigService.search(reqParams,req);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/count")
    @Operation(
            summary = "Count system configurations",
            description = "Returns the total count of system configurations.",
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
        String activity = "Count system configurations";

        LogUtil.logActivity(req, activity, "Success: Count system configurations successfully");
        return new ResponseEntity<>(CountResponse.builder().count(this.systemConfigService.count()).build(), HttpStatus.OK);
    }
}
