package com.jio.consent.controller;

import com.jio.consent.dto.Request.CreateTemplateRequest;
import com.jio.consent.dto.Request.UpdateTemplateRequest;
import com.jio.consent.dto.Response.CountResponse;
import com.jio.consent.dto.Response.SearchResponse;
import com.jio.consent.dto.Response.TemplateDetailsResponse;
import com.jio.consent.dto.Response.TemplateResponse;
import com.jio.consent.entity.Template;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.service.TemplateService;
import com.jio.consent.utils.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/v1.0/templates")
@Tag(name = "Template Management System", description = "Operations pertaining to templates")
public class TemplateController {

    TemplateService templateService;
    Utils utils;

    @Autowired
    public TemplateController(TemplateService templateService, Utils utils) {
        this.templateService = templateService;
        this.utils = utils;
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new template",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Template created successfully",
                            content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @org.springframework.web.bind.annotation.RequestBody CreateTemplateRequest createTemplateRequest) throws IOException, ConsentException {
        Template template = this.templateService.createTemplate(createTemplateRequest);
        return new ResponseEntity<>(TemplateResponse.builder().templateId(template.getTemplateId()).message("Template Created successfully!").build(), HttpStatus.CREATED);
    }

    @PutMapping("/update/{templateId}")
    @Operation(summary = "Update an existing template",
            requestBody = @RequestBody(description = "Request body for updating a template", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateTemplateRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Template updated successfully",
                            content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Template not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<TemplateResponse> updateTemplate(@Valid @org.springframework.web.bind.annotation.RequestBody UpdateTemplateRequest updateTemplateRequest, @PathVariable("templateId") String templateId) throws IOException, ConsentException {
        Template template = this.templateService.updateTemplate(updateTemplateRequest, templateId);
        return new ResponseEntity<>(TemplateResponse.builder().templateId(template.getTemplateId()).message("Template Updated successfully!").build(), HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "Search for templates",
            description = "Search and filter templates based on query parameters. Both tenant-id and txn headers are mandatory.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID) ", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "templateId", description = "TemplateId (UUID)", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "businessId", description = "BusinessId (UUID)", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "version", description = "Template version", required = false, in = ParameterIn.QUERY, example = "1"),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Templates retrieved successfully",
                            content = @Content(schema = @Schema(implementation = SearchResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - missing mandatory headers"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<SearchResponse<Template>> search(@Parameter(hidden = true) @RequestParam Map<String, Object> reqParams) throws ConsentException {
        SearchResponse<Template> templates = this.templateService.searchTemplates(reqParams);
        return new ResponseEntity<>(templates, HttpStatus.OK);
    }

    @GetMapping("/count")
    @Operation(summary = "Get count of templates",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template count retrieved successfully",
                            content = @Content(schema = @Schema(implementation = CountResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<CountResponse> count() {
        return new ResponseEntity<>(CountResponse.builder().count(this.templateService.count()).build(), HttpStatus.OK);
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "Get template details by templateId",
            description = "Fetches all templates for the given templateId and returns the latest version. Returns preferences with embedded Purpose and ProcessorActivity entities, and complete Document entity from documents collection.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "templateId", description = "Template ID (UUID)", required = true, in = ParameterIn.PATH, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template details retrieved successfully",
                            content = @Content(schema = @Schema(implementation = TemplateDetailsResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Template not found"),
                    @ApiResponse(responseCode = "400", description = "Template is in DRAFT status"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<TemplateDetailsResponse> getTemplateDetails(@PathVariable("templateId") String templateId) throws ConsentException {
        Template template = this.templateService.getTemplateDetailsByTemplateId(templateId);
        
        TemplateDetailsResponse response = TemplateDetailsResponse.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .version(template.getVersion())
                .businessId(template.getBusinessId())
                .status(template.getStatus())
                .multilingual(template.getMultilingual())
                .preferences(this.templateService.buildEnhancedPreferences(template))
                .uiConfig(template.getUiConfig())
                .documentMeta(this.templateService.getDocumentFromTemplate(template))
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
