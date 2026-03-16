package com.jio.consent.repository;

import com.jio.consent.entity.Document;

public interface DocumentRepository {

    public Document saveDocument(Document document);

    Document getDocumentById(String documentId);
}
