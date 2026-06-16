package com.jio.digigov.notification.mapper;

import com.jio.digigov.notification.dto.request.event.CreateDataProcessorRequestDto;
import com.jio.digigov.notification.dto.request.event.UpdateDataProcessorRequestDto;
import com.jio.digigov.notification.dto.response.event.DataProcessorResponseDto;
import com.jio.digigov.notification.entity.event.DataProcessor;
import com.jio.digigov.notification.entity.event.DocumentMeta;
import com.jio.digigov.notification.enums.DataProcessorStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataProcessorMapper {

    public DataProcessor toEntity(CreateDataProcessorRequestDto request, String businessId) {
        return DataProcessor.builder()
                .dataProcessorId(request.getDataProcessorId())
                .businessId(businessId)
                .dataProcessorName(request.getDataProcessorName())
                .details(request.getDetails())
                .callbackUrl(request.getCallbackUrl())
                .attachment(request.getAttachment())
                .attachmentMeta(convertToDocumentMeta(request.getAttachmentMeta()))
                .vendorRiskDocument(request.getVendorRiskDocument())
                .vendorRiskDocumentMeta(convertToDocumentMeta(request.getVendorRiskDocumentMeta()))
                .scopeType(request.getScopeType())
                .status(request.getStatus() != null ?
                        DataProcessorStatus.valueOf(request.getStatus()) :
                        DataProcessorStatus.ACTIVE)
                .build();
    }

    public void updateEntity(DataProcessor processor, UpdateDataProcessorRequestDto request) {
        if (request.getDataProcessorName() != null) {
            processor.setDataProcessorName(request.getDataProcessorName());
        }
        if (request.getDetails() != null) {
            processor.setDetails(request.getDetails());
        }
        if (request.getCallbackUrl() != null) {
            processor.setCallbackUrl(request.getCallbackUrl());
        }
        if (request.getAttachment() != null) {
            processor.setAttachment(request.getAttachment());
        }
        if (request.getAttachmentMeta() != null) {
            processor.setAttachmentMeta(convertToDocumentMeta(request.getAttachmentMeta()));
        }
        if (request.getVendorRiskDocument() != null) {
            processor.setVendorRiskDocument(request.getVendorRiskDocument());
        }
        if (request.getVendorRiskDocumentMeta() != null) {
            processor.setVendorRiskDocumentMeta(convertToDocumentMeta(request.getVendorRiskDocumentMeta()));
        }
        if (request.getScopeType() != null) {
            processor.setScopeType(request.getScopeType());
        }
        if (request.getStatus() != null) {
            processor.setStatus(DataProcessorStatus.valueOf(request.getStatus()));
        }
        processor.setUpdatedAt(LocalDateTime.now());
    }

    public DataProcessorResponseDto toResponse(DataProcessor processor) {
        return DataProcessorResponseDto.builder()
                .dataProcessorId(processor.getDataProcessorId())
                .businessId(processor.getBusinessId())
                .dataProcessorName(processor.getDataProcessorName())
                .details(processor.getDetails())
                .callbackUrl(processor.getCallbackUrl())
                .attachment(processor.getAttachment())
                .attachmentMeta(processor.getAttachmentMeta())
                .vendorRiskDocument(processor.getVendorRiskDocument())
                .vendorRiskDocumentMeta(processor.getVendorRiskDocumentMeta())
                .scopeType(processor.getScopeType())
                .status(processor.getStatus().name())
                .createdAt(processor.getCreatedAt())
                .updatedAt(processor.getUpdatedAt())
                .build();
    }

    public String generateProcessorId() {
        return UUID.randomUUID().toString();
    }

    private DocumentMeta convertToDocumentMeta(Object metaObject) {
        if (metaObject == null) {
            return null;
        }
        // This would need proper conversion logic based on the actual structure
        // For now, returning null - implement based on your specific requirements
        return null;
    }
}