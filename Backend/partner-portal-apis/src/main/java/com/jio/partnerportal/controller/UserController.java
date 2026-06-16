package com.jio.partnerportal.controller;

import com.jio.partnerportal.dto.request.UserRequest;
import com.jio.partnerportal.dto.response.ProfileResponse;
import com.jio.partnerportal.dto.response.UserResponse;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Map;
/**
 *
 *
 * @author Kirte.Bhatt
 *
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Create a new user",
            description = "Creates a new user in the system for the given tenant",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User created successfully",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))
                    ),
            }
    )
    @PostMapping("/create")
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-Id") String tenantId,

            @Parameter(description = "Business identifier", required = true)
            @RequestHeader("business-Id") String businessId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = UserRequest.class))
            )
            @RequestBody  UserRequest request,

            HttpServletRequest req
    ) {
        return ResponseEntity.ok(userService.createUser(txn, tenantId, businessId,sessionToken, request,req));
    }
    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Update an existing user",
            description = "Updates user details for the given tenant and user ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User updated successfully",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))
                    ),
            }
    )
    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-Id") String tenantId,

            @Parameter(description = "Business identifier", required = true)
            @RequestHeader("business-Id") String businessId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,

            @Parameter(description = "User identifier to be updated", required = true)
            @RequestParam String userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User update request payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserRequest.class))
            )
            @RequestBody UserRequest request,
            HttpServletRequest req
    ) {
        return ResponseEntity.ok(
                userService.updateUserResponse(txn, tenantId, businessId, sessionToken, userId, request,req)
        );
    }

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Delete a user",
            description = "Deletes an existing user for the given tenant and user ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User deleted successfully",
                            content = @Content(
                                    schema = @Schema(
                                            implementation = Map.class,
                                            example = "{ \"message\": \"User deleted successfully\" }"
                                    )
                            )
                    ),
            }
    )
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteUser(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-Id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,

            @Parameter(description = "User identifier to be deleted", required = true)
            @RequestParam String userId,
            HttpServletRequest req
    ) throws PartnerPortalException {
        String msg = userService.deleteUser(txn, tenantId, sessionToken, userId,req);
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Search for a user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User found successfully",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))
                    ),
            }
    )
    @GetMapping("/search")
    public ResponseEntity<UserResponse> searchUser(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-Id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,

            @Parameter(description = "User identifier", required = true)
            @RequestParam String userId,

            @Parameter(description = "User email address")
            @RequestParam(required = false) String email,

            @Parameter(description = "User mobile number")
            @RequestParam(required = false) String mobile,
            HttpServletRequest req
    ) {
        return ResponseEntity.ok(userService.searchUser(txn, tenantId, sessionToken, userId, email, mobile,req));
    }
    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Count users",
            description = "Returns the total number of users for the given tenant",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User count retrieved successfully",
                            content = @Content(schema = @Schema(
                                    example = "{ \"message\": \"2 users found\" }"
                            ))
                    ),
            }
    )
    @GetMapping("/count")
    public ResponseEntity<Map<String, String>> countUsers(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-Id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,
            HttpServletRequest req
    ) {
        String msg = userService.countUsers(txn, tenantId, sessionToken,req);
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "List all users",
            description = "Fetches all users for the given tenant",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of users retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))
                    ),
            }
    )
    @GetMapping("/list")
    public ResponseEntity<List<UserResponse>> listUsers(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-Id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,
            HttpServletRequest req
    ) {
        List<UserResponse> users = userService.listUsers(txn, tenantId, sessionToken,req);
        return ResponseEntity.ok(users);
    }

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Get user profile",
            description = "Retrieves the profile details of the current user based on the session token and tenant",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Profile retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
                    ),
            }
    )
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,
            HttpServletRequest req
    ) {
        return userService.getProfile(txn, tenantId, sessionToken,req);
    }



}

