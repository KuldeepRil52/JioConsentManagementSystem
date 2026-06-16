package com.jio.digigov.grievance.repository;

import com.jio.digigov.grievance.entity.DocumentEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentRepository extends MongoRepository<DocumentEntity, ObjectId> {
    DocumentEntity findByDocumentId(String documentId);
}
