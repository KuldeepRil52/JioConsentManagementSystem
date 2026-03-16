package com.jio.partnerportal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.dto.request.RopaRequest;
import com.jio.partnerportal.dto.response.RopaCreateResponse;
import com.jio.partnerportal.dto.response.RopaDeleteResponse;
import com.jio.partnerportal.dto.response.RopaUpdateResponse;
import com.jio.partnerportal.dto.response.RopaDetailResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.dto.response.CountResponse;
import com.jio.partnerportal.dto.ErrorResponse;
import com.jio.partnerportal.entity.RopaRecord;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.RopaService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1.0/ropa")
public class RopaController {

    Validation validation;
    RopaService ropaService;

    @Autowired
    public RopaController(Validation validation, RopaService ropaService) {
        this.validation = validation;
        this.ropaService = ropaService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create")
    @Operation(
            summary = "Create ROPA record",
            description = "Creates a new Record of Processing Activities (ROPA) record.",
            parameters = {
                    @Parameter(name = "business-id", description = "Business ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            requestBody = @RequestBody(
                    description = "Request body for creating a ROPA record",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RopaRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "processingActivityId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                                      "purposeForProcessingId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                                      "categoriesOfSpecialNature": ["Marital Relations", "Physical disabilities"],
                                      "sourceOfPersonalData": ["Recruitment agencies"],
                                      "categoryOfIndividual": ["Employment Candidates"],
                                      "activityReason": "Explicit consent",
                                      "additionalCondition": "Employment",
                                      "caseOrPurposeForExemption": "N/A",
                                      "dpiaReference": "DPIA#00xx",
                                      "linkOrDocumentRef": null,
                                      "businessFunctionsSharedWith": ["HR/Recruitment"],
                                      "geographicalLocations": ["India"],
                                      "thirdPartiesSharedWith": ["Recruitment agency [Name]"],
                                      "contractReferences": ["Contract#00xx"],
                                      "crossBorderFlow": false,
                                      "restrictedTransferSafeguards": "N/A",
                                      "administrativePrecautions": "Contractual obligations",
                                      "financialPrecautions": "Vendor audits",
                                      "technicalPrecautions": "Internal data storage and retention, strict access control",
                                      "retentionPeriod": {
                                        "value": 2,
                                        "unit": "YEARS"
                                      },
                                      "storageLocation": "Shared drives and emails",
                                      "breachDocumentation": "N/A",
                                      "lastBreachDate": "2024-01-15T10:30:00.000",
                                      "breachSummary": null
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "ROPA record created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RopaCreateResponse.class))
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
                                                  "errorMessage": "Validation failed: businessId cannot be null",
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
    public ResponseEntity<RopaCreateResponse> createRopa(@org.springframework.web.bind.annotation.RequestBody RopaRequest request, @RequestHeader Map<String, String> headers, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        this.validation.validateRopaHeader(headers);
        RopaRecord ropaRecord = this.ropaService.createRopa(headers, request, req);
        return new ResponseEntity<>(RopaCreateResponse.builder().ropaId(ropaRecord.getRopaId()).message("ROPA record created successfully!").build(), HttpStatus.CREATED);
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/update/{ropaId}")
    @Operation(
            summary = "Update ROPA record",
            description = "Updates an existing ROPA record by its ID.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
                    @Parameter(name = "ropaId", description = "ID of the ROPA record to update", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"))
            },
            requestBody = @RequestBody(
                    description = "Request body for updating a ROPA record",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RopaRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "processingActivityId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                                      "purposeForProcessingId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                                      "categoriesOfSpecialNature": ["Marital Relations", "Physical disabilities"],
                                      "sourceOfPersonalData": ["Recruitment agencies"],
                                      "categoryOfIndividual": ["Employment Candidates"],
                                      "activityReason": "Explicit consent",
                                      "additionalCondition": "Employment",
                                      "caseOrPurposeForExemption": "N/A",
                                      "dpiaReference": "DPIA#00xx",
                                      "linkOrDocumentRef": null,
                                      "businessFunctionsSharedWith": ["HR/Recruitment"],
                                      "geographicalLocations": ["India"],
                                      "thirdPartiesSharedWith": ["Recruitment agency [Name]"],
                                      "contractReferences": ["Contract#00xx"],
                                      "crossBorderFlow": false,
                                      "restrictedTransferSafeguards": "N/A",
                                      "administrativePrecautions": "Contractual obligations",
                                      "financialPrecautions": "Vendor audits",
                                      "technicalPrecautions": "Internal data storage and retention, strict access control",
                                      "retentionPeriod": {
                                        "value": 2,
                                        "unit": "YEARS"
                                      },
                                      "storageLocation": "Shared drives and emails",
                                      "breachDocumentation": "N/A",
                                      "lastBreachDate": "2024-01-15T10:30:00.000",
                                      "breachSummary": null
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ROPA record updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RopaUpdateResponse.class))
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
                                                  "errorMessage": "Validation failed: businessId cannot be null",
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
    public ResponseEntity<RopaUpdateResponse> updateRopa(@org.springframework.web.bind.annotation.RequestBody RopaRequest request, @RequestHeader Map<String, String> headers, @PathVariable("ropaId") String ropaId, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        RopaRecord ropaRecord = this.ropaService.updateRopa(ropaId, request, req);
        return new ResponseEntity<>(RopaUpdateResponse.builder().ropaId(ropaRecord.getRopaId()).message("ROPA record updated successfully!").build(), HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/{ropaId}")
    @Operation(
            summary = "Get ROPA record by ID",
            description = "Retrieves a ROPA record with detailed process overview by its ID.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
                    @Parameter(name = "ropaId", description = "ID of the ROPA record to retrieve", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ROPA record retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RopaDetailResponse.class))
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
                                                  "errorMessage": "Validation failed: businessId cannot be null",
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
    public ResponseEntity<RopaDetailResponse> getRopaById(@PathVariable("ropaId") String ropaId, @RequestHeader Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        RopaDetailResponse ropaDetailResponse = this.ropaService.getRopaDetails(ropaId, tenantId,req);
        return new ResponseEntity<>(ropaDetailResponse, HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping("/delete/{ropaId}")
    @Operation(
            summary = "Delete ROPA record",
            description = "Deletes a ROPA record by its ID.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
                    @Parameter(name = "ropaId", description = "ID of the ROPA record to delete", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ROPA record deleted successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RopaDeleteResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "ropaId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                                              "message": "ROPA record deleted successfully!"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "ROPA record not found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "ROPA record not found",
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
    public ResponseEntity<RopaDeleteResponse> deleteRopa(@PathVariable("ropaId") String ropaId, HttpServletRequest req) throws PartnerPortalException {
        this.ropaService.deleteRopa(ropaId, req);
        return new ResponseEntity<>(RopaDeleteResponse.builder().ropaId(ropaId).message("ROPA record deleted successfully!").build(), HttpStatus.NO_CONTENT);
    }

    @CrossOrigin(origins = "*")
    @GetMapping
    @Operation(
            summary = "Get all ROPA records by tenant",
            description = "Retrieves all ROPA records for the tenant with detailed process overview. Uses optimized bulk queries.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ROPA records retrieved successfully",
                            content = @Content(mediaType = "application/json", 
                                    schema = @Schema(implementation = SearchResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "searchList": [
                                                {
                                                  "id": "507f1f77bcf86cd799439011",
                                                  "ropaId": "c3d4e5f6-g7h8-9012-3456-789012cdefgh",
                                                  "businessId": "d4e5f6g7-h8i9-0123-4567-890123defghi",
                                                  "processOverview": {
                                                    "businessFunction": "Human Resources",
                                                    "department": "Human Resources",
                                                    "processOwner": {
                                                      "name": "John Doe",
                                                      "mobile": "+91-9876543210",
                                                      "email": "john.doe@company.com"
                                                    },
                                                    "processingActivityName": "Recruitment",
                                                    "purposeForProcessing": "To hire and recruit new employees"
                                                  },
                                                  "categoriesOfSpecialNature": ["Marital Relations", "Physical disabilities"],
                                                  "sourceOfPersonalData": ["Recruitment agencies"],
                                                  "categoryOfIndividual": ["Employment Candidates"],
                                                  "activityReason": "Explicit consent",
                                                  "additionalCondition": "Employment",
                                                  "caseOrPurposeForExemption": "N/A",
                                                  "dpiaReference": "DPIA#00xx",
                                                  "linkOrDocumentRef": null,
                                                  "businessFunctionsSharedWith": ["HR/Recruitment"],
                                                  "geographicalLocations": ["India"],
                                                  "thirdPartiesSharedWith": ["Recruitment agency [Name]"],
                                                  "contractReferences": ["Contract#00xx"],
                                                  "crossBorderFlow": false,
                                                  "restrictedTransferSafeguards": "N/A",
                                                  "administrativePrecautions": "Contractual obligations",
                                                  "financialPrecautions": "Vendor audits",
                                                  "technicalPrecautions": "Internal data storage and retention, strict access control",
                                                  "retentionPeriod": {
                                                    "value": 2,
                                                    "unit": "YEARS"
                                                  },
                                                  "storageLocation": "Shared drives and emails",
                                                  "breachDocumentation": "N/A",
                                                  "lastBreachDate": "2024-01-15T10:30:00.000",
                                                  "breachSummary": null,
                                                  "createdAt": "2024-01-20T09:15:30.123",
                                                  "updatedAt": "2024-01-20T09:15:30.123"
                                                }
                                              ]
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<SearchResponse<RopaDetailResponse>> getAllRopaRecords(@RequestHeader Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        List<RopaDetailResponse> ropaDetails = this.ropaService.getAllRopaDetails(tenantId,req);
        return new ResponseEntity<>(SearchResponse.<RopaDetailResponse>builder().searchList(ropaDetails).build(), HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/count")
    @Operation(
            summary = "Count ROPA records",
            description = "Returns the total count of ROPA records.",
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
                                                  "errorMessage": "Validation failed: businessId cannot be null",
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
        String activity = "Count ROPA records";

        LogUtil.logActivity(req, activity, "Success: Count ROPA records successfully");
        return new ResponseEntity<>(CountResponse.builder().count(this.ropaService.count()).build(), HttpStatus.OK);
    }

}
