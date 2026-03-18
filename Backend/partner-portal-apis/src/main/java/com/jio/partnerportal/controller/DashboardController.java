package com.jio.partnerportal.controller;

import com.jio.partnerportal.dto.response.DashboardResponse;
import com.jio.partnerportal.dto.response.DataProcessorListResponse;
import com.jio.partnerportal.dto.response.UserListResponse;
import com.jio.partnerportal.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1.0/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/data")
    @Operation(
            summary = "Get dashboard data",
            description = "Fetches all dashboard data organized by business name including businesses, processors, processor activities, users, and roles for the given tenant.",
            parameters = {
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, 
                            schema = @Schema(type = "string", format = "uuid", example = "566d6143-a5e2-47f6-91ae-e271e2105000")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, 
                            schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, 
                            in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Dashboard data retrieved successfully",
                            content = @Content(schema = @Schema(implementation = DashboardResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = com.jio.partnerportal.dto.ErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<DashboardResponse> getDashboardData(
            @RequestHeader("tenant-id") String tenantId,
            HttpServletRequest req) {
        DashboardResponse response = dashboardService.getDashboardData(tenantId, req);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/data-processors")
    @Operation(
            summary = "Get data processor list",
            description = "Fetches all data processors with their activities. Data is not sorted by business.",
            parameters = {
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, 
                            schema = @Schema(type = "string", format = "uuid", example = "566d6143-a5e2-47f6-91ae-e271e2105000")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, 
                            schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, 
                            in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Data processor list retrieved successfully",
                            content = @Content(schema = @Schema(implementation = DataProcessorListResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = com.jio.partnerportal.dto.ErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<DataProcessorListResponse> getDataProcessorList(
            @RequestHeader("tenant-id") String tenantId,
            HttpServletRequest req) {
        DataProcessorListResponse response = dashboardService.getDataProcessorList(tenantId, req);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/users")
    @Operation(
            summary = "Get user list",
            description = "Fetches all users with their associated roles and permissions.",
            parameters = {
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, 
                            schema = @Schema(type = "string", format = "uuid", example = "566d6143-a5e2-47f6-91ae-e271e2105000")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, 
                            schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, 
                            in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User list retrieved successfully",
                            content = @Content(schema = @Schema(implementation = UserListResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = com.jio.partnerportal.dto.ErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<UserListResponse> getUserList(
            @RequestHeader("tenant-id") String tenantId,
            HttpServletRequest req) {
        UserListResponse response = dashboardService.getUserList(tenantId, req);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

