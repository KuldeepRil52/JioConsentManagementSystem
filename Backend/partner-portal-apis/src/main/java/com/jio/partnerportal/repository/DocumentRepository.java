package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.Document;

public interface DocumentRepository {

    public Document saveDocument(Document document);

    Document getDocumentById(String documentId);

}
