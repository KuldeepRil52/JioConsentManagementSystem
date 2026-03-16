package com.jio.consent.service;

import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.entity.Document;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.repository.DocumentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class DocumentService {

    DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public ResponseEntity<byte[]> viewDocumentById(String documentId) throws ConsentException {
        Document document = this.documentRepository.getDocumentById(documentId);
        if (document.isBase64Document()) {
            String[] dataParts = document.getData().split(",", 2);
            String base64Content = dataParts[1];
            base64Content = base64Content.trim().replaceAll("\\s", "");
            byte[] documentBytes = Base64.getDecoder().decode(base64Content);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, document.getDocumentName()).body(documentBytes);
        }
        return null;
    }


    public Document getDocumentById(String documentId) {
        return this.documentRepository.getDocumentById(documentId);
    }

}
