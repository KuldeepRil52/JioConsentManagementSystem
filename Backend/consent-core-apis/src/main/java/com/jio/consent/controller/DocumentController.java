package com.jio.consent.controller;

import com.jio.consent.entity.Document;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1.0/documents")
@Tag(name = "Document Management System", description = "Operations pertaining to documents")
public class DocumentController {

    DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }


    @GetMapping("view-document/{documentId}")
    @Operation(summary = "View a document by ID",
            parameters = {
                    @Parameter(name = "documentId", description = "Document ID", required = true, example = "doc_123"),
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Document retrieved successfully",
                            content = @Content(mediaType = "application/octet-stream"))
            }
    )
        public ResponseEntity<byte[]> viewDocument(@PathVariable("documentId") String documentId) throws ConsentException {
            return this.documentService.viewDocumentById(documentId);
    }

    @GetMapping("get-document/{documentId}")
    @Operation(summary = "Get document details by ID",
            parameters = {
                    @Parameter(name = "documentId", description = "Document ID", required = true, example = "doc_123"),
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Document details retrieved successfully",
                            content = @Content(schema = @Schema(implementation = Document.class))),
                    @ApiResponse(responseCode = "404", description = "Document not found")
            }
    )
    public ResponseEntity<Document> getDocument(@PathVariable("documentId") String documentId) {
        return new ResponseEntity<>(this.documentService.getDocumentById(documentId), HttpStatus.OK);
    }
}
