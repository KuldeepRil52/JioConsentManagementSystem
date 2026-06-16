package com.jio.auth.docs;

import com.jio.auth.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


import java.lang.annotation.*;


public final class SessionDocs {

    private SessionDocs() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Create Secure Code Session",
            description = "Creates a secure session for static token validation",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = """
                                            {
                                              "tenantId": "0076a4b4-04ef-44c6-8519-f38437a34398",
                                              "businessId": "0076a4b4-04ef-44c6-8519-f38437a34398",
                                              "identity": "8104983372",
                                              "identityType": "MOBILE"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Session created successfully",
                            content = @Content(schema = @Schema(implementation = SessionResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public @interface Create {}


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Validate Secure Code Session",
            description = "Validates a session using AccessToken, identity, tenantId, and businessId headers",
            parameters = {
                    @Parameter(name = "AccessToken", in = ParameterIn.HEADER, required = true),
                    @Parameter(name = "identity", in = ParameterIn.HEADER, required = true),
                    @Parameter(name = "tenantId", in = ParameterIn.HEADER, required = true),
                    @Parameter(name = "businessId", in = ParameterIn.HEADER, required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Session validated successfully",
                            content = @Content(schema = @Schema(implementation = SessionStatusResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid or expired token",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public @interface Validate {}


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Check Access Token Only",
            description = "Checks only AccessToken validity from headers",
            parameters = {
                    @Parameter(name = "AccessToken", in = ParameterIn.HEADER, required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Access token valid",
                            content = @Content(schema = @Schema(implementation = SessionStatusResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Access token invalid or expired",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public @interface CheckAccessToken {}
}
