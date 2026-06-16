package com.example.scanner.controller;

import com.example.scanner.dto.request.AddCookieCategoryRequest;
import com.example.scanner.dto.request.UpdateCookieCategoryRequest;
import com.example.scanner.dto.response.CookieCategoryResponse;
import com.example.scanner.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Cookie Category Management", description = "APIs for managing cookie categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Add a new cookie category",
            description = """
                Creates a new cookie category for the specified tenant.
                
                Error Codes: R4001 (Validation error), R4009 (Category exists), R5000 (Internal error)
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Category created successfully",
                    content = @Content(
                            schema = @Schema(implementation = CookieCategoryResponse.class),
                            examples = @ExampleObject(value = """
                                {"success": true, "message": "Category added successfully", "categoryName": "Analytics"}
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - validation failed or category already exists",
                    content = @Content(
                            schema = @Schema(implementation = CookieCategoryResponse.class),
                            examples = @ExampleObject(value = """
                                {"success": false, "message": "Category already exists", "errorCode": "R4009"}
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            schema = @Schema(implementation = CookieCategoryResponse.class),
                            examples = @ExampleObject(value = """
                                {"success": false, "message": "Internal server error", "errorCode": "R5000"}
                                """)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    schema = @Schema(implementation = AddCookieCategoryRequest.class),
                    examples = @ExampleObject(value = """
                        {"categoryName": "Analytics", "description": "Cookies for analytics"}
                        """)
            )
    )
    public ResponseEntity<CookieCategoryResponse> addCategory(
            @Parameter(description = "Tenant ID", required = true, example = "cst_123e4567-e89b-XXX....")
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody AddCookieCategoryRequest request) {

        if (tenantId == null || tenantId.trim().isEmpty()) {
            CookieCategoryResponse errorResponse = CookieCategoryResponse.builder()
                    .success(false)
                    .message("Tenant ID is required in header")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        CookieCategoryResponse response = categoryService.addCategory(request, tenantId);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Update an existing cookie category",
            description = """
                Updates the description of an existing cookie category for the specified tenant.
                
                Error Codes: R4001 (Validation error), R4041 (Not found), R5000 (Internal error)
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Category updated successfully",
                    content = @Content(
                            schema = @Schema(implementation = CookieCategoryResponse.class),
                            examples = @ExampleObject(value = """
                                {"success": true, "message": "Category updated successfully"}
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - validation failed",
                    content = @Content(schema = @Schema(implementation = CookieCategoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found",
                    content = @Content(
                            schema = @Schema(implementation = CookieCategoryResponse.class),
                            examples = @ExampleObject(value = """
                                {"success": false, "message": "Category not found", "errorCode": "R4041"}
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = CookieCategoryResponse.class))
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    schema = @Schema(implementation = UpdateCookieCategoryRequest.class),
                    examples = @ExampleObject(value = """
                        {"categoryName": "Analytics", "description": "Updated description"}
                        """)
            )
    )
    public ResponseEntity<CookieCategoryResponse> updateCookieCategory(
            @Parameter(description = "Tenant ID", required = true, example = "cst_123e4567-e89b-XXX....")
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody UpdateCookieCategoryRequest request) {

        if (tenantId == null || tenantId.trim().isEmpty()) {
            CookieCategoryResponse errorResponse = CookieCategoryResponse.builder()
                    .success(false)
                    .message("Tenant ID is required in header")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        CookieCategoryResponse response = categoryService.updateCookieCategory(request, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping()
    @Operation(
            summary = "Fetch all cookie categories",
            description = "Retrieves all cookie categories for the specified tenant"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Categories fetched successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CookieCategoryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No categories found",
                    content = @Content(schema = @Schema(implementation = CookieCategoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = CookieCategoryResponse.class))
            )
    })
    public ResponseEntity<List<CookieCategoryResponse>> getAllCategory(
            @Parameter(description = "Tenant ID", required = true, example = "cst_123e4567-e89b-XXX....")
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId){
        List<CookieCategoryResponse> categoryList = categoryService.findAll(tenantId);
        if(categoryList.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(categoryList);
        }
        return ResponseEntity.status(HttpStatus.OK).body(categoryList);
    }

}