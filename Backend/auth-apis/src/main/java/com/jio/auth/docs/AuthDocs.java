package com.jio.auth.docs;

import com.jio.auth.dto.*;
import com.jio.auth.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

public final class AuthDocs {

    private AuthDocs() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Introspect a JWT token",
            description = "Validates the JWT token and returns claims/payload along with validity",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Token introspected successfully",
                            content = @Content(schema = @Schema(implementation = IntrospectResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid Request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized request",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\"active\": false}")
                            )
                    )
            }
    )
    public @interface Introspect {}



    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Revoke a JWT token",
            description = "Revokes the JWT token",
            parameters = {
                    @Parameter(name = "x-session-token", in = ParameterIn.HEADER, required = true,
                            description = "JWT token issued by IdP",
                            example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Token revoked successfully",
                            content = @Content(schema = @Schema(implementation = RevokeResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid Request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public @interface Revoke {}


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Generate Token",
            description = "Generate JWT token and returns JWT accessToken with validity. Payload should contain iss and sub which is not validated at auth handler end",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = """
                                        {
                                          "iss": "partner-portal",
                                          "tenantId": "6d30e699-31f5-4f28-9dca-0109ea81a90d",
                                          "businessId": "6d30e699-31f5-4f28-9dca-0109ea81a90d",
                                          "sub": "2ae3b9eb-b2c7-4bb0-959f-42b242e5d77c"
                                        }
                                        """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Token generated successfully",
                            content = @Content(schema = @Schema(implementation = JwtResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public @interface Generate {}


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Validate Token",
            description = "Validates a JWT token for specific businessId and returns details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Token validated successfully",
                            content = @Content(schema = @Schema(implementation = IntrospectResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public @interface Validate {}
}
