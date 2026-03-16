package com.jio.partnerportal.controller;

import com.jio.partnerportal.dto.request.RoleRequest;
import com.jio.partnerportal.dto.request.RoleUpdateRequest;
import com.jio.partnerportal.dto.response.RoleResponse;
import com.jio.partnerportal.dto.response.RoleUpdateResponse;
import com.jio.partnerportal.entity.Component;
import com.jio.partnerportal.entity.Role;
import com.jio.partnerportal.service.UserRolesService;
import com.jio.partnerportal.util.AuthUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/role")
@RequiredArgsConstructor
public class UserRolesController {

    private final UserRolesService userRolesService;

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Create a new role",
            description = "Creates a new role for the given tenant and session",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Role creation request payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RoleRequest.class),
                            examples = {
                                    @ExampleObject(
                                            value = """
                                                    {
                                                       "role": "demo",
                                                       "description": "testing role",
                                                       "permissions": [
                                                         {
                                                           "componentId": "b3a3a7e1-7e6c-4e0d-9187-8e9f5c2eaa01",
                                                           "action": ["WRITE"]
                                                         },
                                                         {
                                                           "componentId": "6f47e9e4-1c3b-4f92-bc78-0c3bb4d5e61f",
                                                           "action": ["READ"]
                                                         }
                                                       ]
                                                     }
                                                    
                    """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role created successfully",
                            content = @Content(schema = @Schema(implementation = RoleResponse.class))
                    )
            }
    )
    @PostMapping("/create")
    public ResponseEntity<RoleResponse> createRole(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Role creation request payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RoleRequest.class))
            )
            @RequestBody RoleRequest request, HttpServletRequest req
    ) {
        RoleResponse response = userRolesService.createRole(request, txn, tenantId, sessionToken,req);
        return ResponseEntity.ok(response);
    }
    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Update an existing role",
            description = "Updates role details for the given tenant and role ID",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Role update request payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RoleRequest.class),
                            examples = {
                                    @ExampleObject(
                                            value = """
                                                    {
                                                       "role": "Update Role",
                                                       "description": "Update description",
                                                       "permissions": [
                                                         {
                                                           "componentId": "b3a3a7e1-7e6c-4e0d-9187-8e9f5c2eaa01",
                                                           "action": ["WRITE","READ"]
                                                         },
                                                         {
                                                           "componentId": "6f47e9e4-1c3b-4f92-bc78-0c3bb4d5e61f",
                                                           "action": ["WRITE"]
                                                         }
                                                       ]
                                                     }
                                                    
                    """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role updated successfully",
                            content = @Content(schema = @Schema(implementation = RoleUpdateResponse.class))
                    ),
            }
    )
    @PutMapping("/update")
    public ResponseEntity<RoleUpdateResponse> updateRole(
            @Parameter(description = "Role identifier to be updated", required = true)
            @RequestParam String roleId,

            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Role update request payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RoleUpdateRequest.class))
            )
            @RequestBody RoleUpdateRequest request, HttpServletRequest req
    ) {
        return ResponseEntity.ok(
                userRolesService.updateRole(roleId, request, txn,tenantId, sessionToken,req)
        );
    }

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Delete a role",
            description = "Deletes a role for the given tenant and role ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role deleted successfully",
                            content = @Content(schema = @Schema(implementation = Map.class))
                    ),
            }
    )
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteRole(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,

            @Parameter(description = "Role identifier to be deleted", required = true)
            @RequestParam("roleId") String roleId,
            HttpServletRequest req
    ) {
        return userRolesService.deleteRole(txn, tenantId,sessionToken, roleId,req);
    }


    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Search for roles",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role search successful",
                            content = @Content(schema = @Schema(implementation = RoleResponse.class))
                    ),
            }
    )
    @GetMapping("/search")
    public ResponseEntity<RoleResponse> searchRole(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,

            @Parameter(description = "Role identifier to search for")
            @RequestParam(value = "roleId", required = false) String roleId,

            @Parameter(description = "Role name to search for")
            @RequestParam(value = "role", required = false) String role,
            HttpServletRequest req
    ) {
        return userRolesService.searchRole(txn,tenantId, sessionToken, roleId, role,req);
    }

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Count roles",
            description = "Returns the total number of roles for the given tenant",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role count retrieved successfully",
                            content = @Content(schema = @Schema(
                                    example = "{ \"message\": \"10 roles found\" }",
                                    type = "object"
                            ))
                    ),
            }
    )
    @GetMapping("/count")
    public ResponseEntity<Map<String, String>> countRoles(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,
            HttpServletRequest req
    ) {
        String message = userRolesService.countRoles(txn,tenantId, sessionToken,req);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "List all roles",
            description = "Fetches all roles for the given tenant",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of roles retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Role.class)))
                    ),
            }
    )
    @GetMapping("/list")
    public ResponseEntity<List<Role>> listRoles(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,
            HttpServletRequest req
    ) {
        return ResponseEntity.ok(userRolesService.listRoles(txn,tenantId, sessionToken,req));
    }

    @GetMapping("/component/list")
    public ResponseEntity<List<Component>> listComponents(
            @Parameter(description = "Transaction ID for request tracking", required = true)
            @RequestHeader("txn") String txn,

            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("tenant-Id") String tenantId,

            @Parameter(description = "Session authentication token", required = true)
            @RequestHeader("x-session-token") String sessionToken,
            HttpServletRequest req
    ) {
        // Call service to fetch tenant-specific components
        List<Component> components = userRolesService.listComponents(txn, tenantId, sessionToken,req);
        return ResponseEntity.ok(components);
    }
}




