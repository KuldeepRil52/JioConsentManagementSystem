package com.jio.partnerportal.controller;

import com.jio.partnerportal.dto.request.PurposeRequest;
import com.jio.partnerportal.dto.response.CountResponse;
import com.jio.partnerportal.dto.response.PurposeResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.dto.ErrorResponse;
import com.jio.partnerportal.entity.Purpose;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.PurposeService;
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
@RequestMapping("/v1.0/purpose")
public class PurposeController {
    Validation validation;
    PurposeService purposeService;

    @Autowired
    public PurposeController(Validation validation, PurposeService purposeService) {
        this.validation = validation;
        this.purposeService = purposeService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create")
    @Operation(
            summary = "Create purpose",
            description = "Creates a new purpose.",
            parameters = {
                    @Parameter(name = "business-id", description = "Business ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "scope-level", description = "Scope Level", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"TENANT", "BUSINESS"}, example = "TENANT")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            requestBody = @RequestBody(
                    description = "Request body for creating a purpose",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PurposeRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Purpose created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PurposeResponse.class))
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
    public ResponseEntity<PurposeResponse> createPurpose(@org.springframework.web.bind.annotation.RequestBody PurposeRequest request, @RequestHeader Map<String, String> headers, HttpServletRequest req) throws BodyValidationException, PartnerPortalException {
       this.validation.validatePurposeRequest(request, headers);
        Purpose purpose = this.purposeService.createPurpose(request, headers, req);
        return new ResponseEntity<>(PurposeResponse.builder().purposeId(purpose.getPurposeId()).message("Purpose created successfully!").build(), HttpStatus.CREATED);
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/update/{purposeId}")
    @Operation(
            summary = "Update purpose",
            description = "Updates an existing purpose by its ID.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
                    @Parameter(name = "purposeId", description = "ID of the purpose to update", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"))
            },
            requestBody = @RequestBody(
                    description = "Request body for updating a purpose",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PurposeRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Purpose updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PurposeResponse.class))
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
    public ResponseEntity<PurposeResponse> updatePurpose(@org.springframework.web.bind.annotation.RequestBody PurposeRequest request, @RequestHeader Map<String, String> headers, @PathVariable("purposeId") String purposeId, HttpServletRequest req) throws BodyValidationException, PartnerPortalException {
        Purpose purpose = this.purposeService.updatePurpose(request, headers, purposeId, req);
        return new ResponseEntity<>(PurposeResponse.builder().purposeId(purpose.getPurposeId()).message("Purpose updated successfully!").build(), HttpStatus.CREATED);

    }

    @CrossOrigin(origins = "*")
    @GetMapping("/search")
    @Operation(
            summary = "Search purposes",
            description = "Searches for purposes based on provided criteria like purpose ID and name.",
            parameters = {
                    @Parameter(name = "purposeId", description = "ID of the purpose to search for", in = ParameterIn.QUERY, example = "P001"),
                    @Parameter(name = "purposeName", description = "Name of the purpose to search for", in = ParameterIn.QUERY, example = "Data Analysis"),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
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
    public ResponseEntity<SearchResponse<Purpose>> search(@Parameter(hidden = true) @RequestParam Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        SearchResponse<Purpose> response = this.purposeService.search(reqParams, req);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/count")
    @Operation(
            summary = "Count purposes",
            description = "Returns the total count of purposes.",
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
        String activity = "Count purpose";

        LogUtil.logActivity(req, activity, "Success: Count purpose successfully");
        return new ResponseEntity<>(CountResponse.builder().count(this.purposeService.count()).build(), HttpStatus.OK);
    }

}
