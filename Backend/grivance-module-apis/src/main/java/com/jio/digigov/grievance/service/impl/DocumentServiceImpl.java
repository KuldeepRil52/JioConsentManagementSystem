package com.jio.digigov.grievance.service.impl;

import com.jio.digigov.grievance.config.MultiTenantMongoConfig;
import com.jio.digigov.grievance.dto.DocumentMeta;
import com.jio.digigov.grievance.entity.DocumentEntity;
import com.jio.digigov.grievance.entity.Tag;
import com.jio.digigov.grievance.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final MultiTenantMongoConfig mongoConfig;

    @Override
    public DocumentMeta saveBase64Document(String base64, String name, String contentType,
                                           String businessId, String tenantId, Tag tag) {

        // decode to calculate size
        byte[] decoded = Base64.getDecoder().decode(base64);
        long size = decoded.length;

        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        DocumentEntity doc = DocumentEntity.builder()
                .documentId(UUID.randomUUID().toString())
                .businessId(businessId)
                .documentName(name)
                .contentType(contentType)
                .isBase64Document(true)
                .documentSize(size)
                .data(base64)
                .version(1)
                .status("ACTIVE")
                .tag(tag)
                .build();

        DocumentEntity saved = tenantMongoTemplate.save(doc);

        return DocumentMeta.builder()
                .documentId(saved.getDocumentId())
                .name(saved.getDocumentName())
                .contentType(saved.getContentType())
                .size(saved.getDocumentSize())
                .tag(saved.getTag())
                .build();
    }
}
