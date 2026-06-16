package com.jio.digigov.grievance.service;

import com.jio.digigov.grievance.dto.DocumentMeta;
import com.jio.digigov.grievance.entity.DocumentEntity;
import com.jio.digigov.grievance.entity.Tag;

public interface DocumentService {
    DocumentMeta saveBase64Document(String base64, String name, String contentType,
                                    String businessId, String tenantId, Tag tag);
}
