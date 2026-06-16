package com.jio.digigov.notification.service.event.impl;

import com.jio.digigov.notification.dto.request.event.CreateDataProcessorRequestDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.event.DataProcessorResponseDto;
import com.jio.digigov.notification.entity.event.DataProcessor;
import com.jio.digigov.notification.enums.DataProcessorStatus;
import com.jio.digigov.notification.exception.BusinessException;
import com.jio.digigov.notification.exception.ResourceNotFoundException;
import com.jio.digigov.notification.exception.ValidationException;
import com.jio.digigov.notification.mapper.DataProcessorMapper;
import com.jio.digigov.notification.repository.event.DataProcessorRepository;
import com.jio.digigov.notification.service.event.DataProcessorService;
import com.jio.digigov.notification.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataProcessorServiceImpl implements DataProcessorService {

    private final DataProcessorRepository dataProcessorRepository;
    private final DataProcessorMapper dataProcessorMapper;
    private final TenantService tenantService;
    private final MongoTemplate mongoTemplate;

    @Override
    @Transactional
    public DataProcessorResponseDto createDataProcessor(CreateDataProcessorRequestDto request, 
                                                   String tenantId, 
                                                   String businessId, 
                                                   String transactionId) {
        log.info("Creating data processor for tenant: {}, business: {}, transactionId: {}", 
                tenantId, businessId, transactionId);

        // Using injected mongoTemplate - tenant-aware

        // Generate dataProcessorId if not provided
        if (request.getDataProcessorId() == null || request.getDataProcessorId().trim().isEmpty()) {
            String generatedId = dataProcessorMapper.generateProcessorId();
            request.setDataProcessorId(generatedId);
            log.debug("Generated dataProcessorId: {} for data processor", generatedId);
        }

        if (dataProcessorRepository.existsByDataProcessorIdAndBusinessId(request.getDataProcessorId(), businessId, mongoTemplate)) {
            throw new BusinessException("DUPLICATE_DATA_PROCESSOR", "Data processor with ID '" + request.getDataProcessorId() +
                                      "' already exists for business: " + businessId);
        }

        DataProcessor dataProcessor = dataProcessorMapper.toEntity(request, businessId);

        DataProcessor saved = dataProcessorRepository.save(dataProcessor, mongoTemplate);
        
        log.info("Successfully created data processor with ID: {} for business: {}",
                saved.getDataProcessorId(), businessId);
        
        return dataProcessorMapper.toResponse(saved);
    }

    @Override
    public PagedResponseDto<DataProcessorResponseDto> getAllDataProcessors(String tenantId, 
                                                                   String businessId, 
                                                                   String status, 
                                                                   Integer page, 
                                                                   Integer pageSize, 
                                                                   String sort) {
        log.info("Retrieving data processors for tenant: {}, business: {}, status: {}, page: {}, size: {}", 
                tenantId, businessId, status, page, pageSize);

        // Using injected mongoTemplate - tenant-aware

        Pageable pageable = createPageable(page, pageSize, sort);
        Page<DataProcessor> dataProcessorPage;

        if (status != null && !status.isEmpty()) {
            DataProcessorStatus dpStatus = DataProcessorStatus.valueOf(status.toUpperCase());
            List<DataProcessor> filtered = dataProcessorRepository.findByBusinessIdAndStatus(businessId, dpStatus, mongoTemplate);
            
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            List<DataProcessor> pageContent = filtered.subList(start, end);
            
            dataProcessorPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filtered.size());
        } else {
            dataProcessorPage = dataProcessorRepository.findByBusinessId(businessId, pageable, mongoTemplate);
        }

        List<DataProcessorResponseDto> responses = dataProcessorPage.getContent().stream()
                .map(dataProcessorMapper::toResponse)
                .collect(Collectors.toList());

        return PagedResponseDto.<DataProcessorResponseDto>builder()
                .data(responses)
                .pagination(PagedResponseDto.PaginationInfo.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(dataProcessorPage.getTotalElements())
                        .totalPages(dataProcessorPage.getTotalPages())
                        .hasNext(dataProcessorPage.hasNext())
                        .hasPrevious(dataProcessorPage.hasPrevious())
                        .build())
                .build();
    }

    @Override
    public DataProcessorResponseDto getDataProcessorById(String dataProcessorId, String tenantId, String businessId) {
        log.info("Retrieving data processor by ID: {} for tenant: {}, business: {}", dataProcessorId, tenantId, businessId);

        // Using injected mongoTemplate - tenant-aware

        DataProcessor dataProcessor = dataProcessorRepository.findByDataProcessorIdAndBusinessId(dataProcessorId, businessId, mongoTemplate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Data processor not found with ID: " + dataProcessorId + " for business: " + businessId));

        return dataProcessorMapper.toResponse(dataProcessor);
    }

    @Override
    @Transactional
    public DataProcessorResponseDto updateDataProcessor(String dataProcessorId,
                                                   CreateDataProcessorRequestDto request,
                                                   String tenantId,
                                                   String businessId,
                                                   String transactionId) {
        log.info("Updating data processor ID: {} for tenant: {}, business: {}, transactionId: {}",
                dataProcessorId, tenantId, businessId, transactionId);

        // Using injected mongoTemplate - tenant-aware

        DataProcessor existingDataProcessor = dataProcessorRepository.findByDataProcessorIdAndBusinessId(dataProcessorId, businessId, mongoTemplate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Data processor not found with ID: " + dataProcessorId + " for business: " + businessId));

        if (!request.getDataProcessorId().equals(dataProcessorId) &&
            dataProcessorRepository.existsByDataProcessorIdAndBusinessId(request.getDataProcessorId(), businessId, mongoTemplate)) {
            throw new BusinessException("DUPLICATE_DATA_PROCESSOR", "Data processor with new ID '" + request.getDataProcessorId() +
                                      "' already exists for business: " + businessId);
        }

        // Update fields manually from CreateDataProcessorRequestDto
        if (request.getDataProcessorName() != null) {
            existingDataProcessor.setDataProcessorName(request.getDataProcessorName());
        }
        if (request.getDetails() != null) {
            existingDataProcessor.setDetails(request.getDetails());
        }
        if (request.getCallbackUrl() != null) {
            existingDataProcessor.setCallbackUrl(request.getCallbackUrl());
        }
        if (request.getAttachment() != null) {
            existingDataProcessor.setAttachment(request.getAttachment());
        }
        if (request.getVendorRiskDocument() != null) {
            existingDataProcessor.setVendorRiskDocument(request.getVendorRiskDocument());
        }
        if (request.getScopeType() != null) {
            existingDataProcessor.setScopeType(request.getScopeType());
        }
        if (request.getStatus() != null) {
            existingDataProcessor.setStatus(DataProcessorStatus.valueOf(request.getStatus()));
        }
        existingDataProcessor.setUpdatedAt(LocalDateTime.now());

        DataProcessor updated = dataProcessorRepository.save(existingDataProcessor, mongoTemplate);
        
        log.info("Successfully updated data processor with ID: {} for business: {}",
                updated.getDataProcessorId(), businessId);
        
        return dataProcessorMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteDataProcessor(String dataProcessorId, String tenantId, String businessId, String transactionId) {
        log.info("Deleting data processor ID: {} for tenant: {}, business: {}, transactionId: {}",
                dataProcessorId, tenantId, businessId, transactionId);

        // Using injected mongoTemplate - tenant-aware

        if (!dataProcessorRepository.existsByDataProcessorIdAndBusinessId(dataProcessorId, businessId, mongoTemplate)) {
            throw new ResourceNotFoundException(
                    "Data processor not found with ID: " + dataProcessorId + " for business: " + businessId);
        }

        long deletedCount = dataProcessorRepository.deleteByDataProcessorIdAndBusinessId(dataProcessorId, businessId, mongoTemplate);

        if (deletedCount == 0) {
            throw new BusinessException("DELETE_FAILED", "Failed to delete data processor with ID: " + dataProcessorId);
        }

        log.info("Successfully deleted data processor with ID: {} for business: {}", dataProcessorId, businessId);
    }

    @Override
    public CountResponseDto getDataProcessorCount(String tenantId, String businessId, String status) {
        log.info("Getting data processor count for tenant: {}, business: {}, status: {}", 
                tenantId, businessId, status);

        // Using injected mongoTemplate - tenant-aware

        long totalCount = dataProcessorRepository.countByBusinessId(businessId, mongoTemplate);
        
        Map<String, Long> breakdown = Map.of(
            DataProcessorStatus.ACTIVE.name(), dataProcessorRepository.countByBusinessIdAndStatus(businessId, DataProcessorStatus.ACTIVE, mongoTemplate),
            DataProcessorStatus.INACTIVE.name(), dataProcessorRepository.countByBusinessIdAndStatus(businessId, DataProcessorStatus.INACTIVE, mongoTemplate)
        );

        long filteredCount = totalCount;
        if (status != null && !status.isEmpty()) {
            DataProcessorStatus dpStatus = DataProcessorStatus.valueOf(status.toUpperCase());
            filteredCount = dataProcessorRepository.countByBusinessIdAndStatus(businessId, dpStatus, mongoTemplate);
        }

        return CountResponseDto.builder()
                .data(CountResponseDto.CountData.builder()
                        .totalCount(filteredCount)
                        .breakdown(CountResponseDto.CountData.CountBreakdown.builder()
                                .byStatus(breakdown.entrySet().stream()
                                        .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                entry -> entry.getValue().intValue())))
                                .build())
                        .build())
                .build();
    }

    @Override
    public List<DataProcessor> validateDataProcessorIds(List<String> dataProcessorIds, String tenantId, String businessId) {
        log.info("Validating data processor IDs: {} for tenant: {}, business: {}", dataProcessorIds, tenantId, businessId);

        if (dataProcessorIds == null || dataProcessorIds.isEmpty()) {
            throw new ValidationException("Data processor IDs list cannot be null or empty");
        }

        // Using injected mongoTemplate - tenant-aware

        List<DataProcessor> activeDataProcessors = dataProcessorRepository
                .findActiveByDataProcessorIdsAndBusinessId(dataProcessorIds, businessId, mongoTemplate);

        List<String> foundDataProcessorIds = activeDataProcessors.stream()
                .map(DataProcessor::getDataProcessorId)
                .collect(Collectors.toList());

        List<String> missingDataProcessorIds = dataProcessorIds.stream()
                .filter(dataProcessorId -> !foundDataProcessorIds.contains(dataProcessorId))
                .collect(Collectors.toList());

        if (!missingDataProcessorIds.isEmpty()) {
            throw new ValidationException("Invalid or inactive data processor IDs: " + missingDataProcessorIds);
        }

        log.info("Successfully validated {} data processor IDs for business: {}",
                activeDataProcessors.size(), businessId);

        return activeDataProcessors;
    }

    private Pageable createPageable(Integer page, Integer pageSize, String sort) {
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int size = (pageSize != null && pageSize > 0) ? Math.min(pageSize, 100) : 10;

        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            String field = sortParts[0];
            Sort.Direction direction = (sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])) 
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(pageNumber, size, Sort.by(direction, field));
        } else {
            return PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }
}