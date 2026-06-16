package com.jio.digigov.notification.service.event.impl;

import com.jio.digigov.notification.dto.request.event.CreateEventConfigurationRequestDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.event.EventConfigurationResponseDto;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.exception.BusinessException;
import com.jio.digigov.notification.exception.ResourceNotFoundException;
import com.jio.digigov.notification.exception.ValidationException;
import com.jio.digigov.notification.mapper.EventConfigurationMapper;
import com.jio.digigov.notification.repository.event.EventConfigurationRepository;
import com.jio.digigov.notification.service.event.DataProcessorService;
import com.jio.digigov.notification.service.event.EventConfigurationService;
import com.jio.digigov.notification.service.event.EventConfigurationValidationService;
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
public class EventConfigurationServiceImpl implements EventConfigurationService {

    private final EventConfigurationRepository eventConfigurationRepository;
    private final EventConfigurationMapper eventConfigurationMapper;
    private final DataProcessorService dataProcessorService;
    private final TenantService tenantService;
    private final MongoTemplate mongoTemplate;
    private final EventConfigurationValidationService validationService;

    @Override
    @Transactional
    public EventConfigurationResponseDto createEventConfiguration(CreateEventConfigurationRequestDto request,
                                                             String tenantId,
                                                             String businessId,
                                                             String transactionId) {
        log.info("Creating event configuration for tenant: {}, business: {}, eventType: {}, transactionId: {}",
                tenantId, businessId, request.getEventType(), transactionId);

        // Validate request according to business rules
        validationService.validateEventConfiguration(request);

        // Using injected mongoTemplate - tenant-aware

        if (eventConfigurationRepository.existsByBusinessIdAndEventType(businessId, request.getEventType(), mongoTemplate)) {
            throw new BusinessException("DUPLICATE_EVENT_CONFIG", "Event configuration already exists for eventType '" + 
                                      request.getEventType() + "' and business: " + businessId);
        }

        // Data processor validation is handled through the notification configuration

        EventConfiguration eventConfiguration = eventConfigurationMapper.toEntity(request, businessId);
        eventConfiguration.setBusinessId(businessId);
        eventConfiguration.setIsActive(true);
        eventConfiguration.setCreatedAt(LocalDateTime.now());

        EventConfiguration saved = eventConfigurationRepository.save(eventConfiguration, mongoTemplate);
        
        log.info("Successfully created event configuration for eventType: {} and business: {}", 
                saved.getEventType(), businessId);
        
        return eventConfigurationMapper.toResponse(saved);
    }

    @Override
    public PagedResponseDto<EventConfigurationResponseDto> getAllEventConfigurations(String tenantId, 
                                                                              String businessId, 
                                                                              Boolean isActive,
                                                                              String priority,
                                                                              Integer page, 
                                                                              Integer pageSize, 
                                                                              String sort) {
        log.info("Retrieving event configurations for tenant: {}, business: {}, isActive: {}, priority: {}", 
                tenantId, businessId, isActive, priority);

        // Using injected mongoTemplate - tenant-aware

        Pageable pageable = createPageable(page, pageSize, sort);
        List<EventConfiguration> allConfigurations = eventConfigurationRepository.findByBusinessId(businessId, mongoTemplate);
        
        List<EventConfiguration> filteredConfigurations = allConfigurations.stream()
                .filter(config -> isActive == null || config.getIsActive().equals(isActive))
                .filter(config -> priority == null || priority.isEmpty() || 
                        config.getPriority().equals(EventPriority.valueOf(priority.toUpperCase())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredConfigurations.size());
        List<EventConfiguration> pageContent = filteredConfigurations.subList(start, end);

        List<EventConfigurationResponseDto> responses = pageContent.stream()
                .map(eventConfigurationMapper::toResponse)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) filteredConfigurations.size() / pageable.getPageSize());

        PagedResponseDto.PaginationInfo paginationInfo = PagedResponseDto.PaginationInfo.builder()
                .page(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalItems(filteredConfigurations.size())
                .totalPages(totalPages)
                .hasNext(end < filteredConfigurations.size())
                .hasPrevious(start > 0)
                .build();

        return PagedResponseDto.<EventConfigurationResponseDto>builder()
                .data(responses)
                .pagination(paginationInfo)
                .build();
    }

    @Override
    public EventConfigurationResponseDto getEventConfigurationByEventType(String businessId, 
                                                                      String eventType, 
                                                                      String tenantId) {
        log.info("Retrieving event configuration for eventType: {}, tenant: {}, business: {}",
                eventType, tenantId, businessId);

        // Using injected mongoTemplate - tenant-aware

        EventConfiguration eventConfiguration = eventConfigurationRepository
                .findByBusinessIdAndEventType(businessId, eventType, mongoTemplate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event configuration not found for eventType: " + eventType + " and business: " + businessId));

        return eventConfigurationMapper.toResponse(eventConfiguration);
    }

    @Override
    @Transactional
    public EventConfigurationResponseDto updateEventConfiguration(String businessId,
                                                             String eventType,
                                                             CreateEventConfigurationRequestDto request,
                                                             String tenantId,
                                                             String transactionId) {
        log.info("Updating event configuration for eventType: {}, tenant: {}, business: {}, transactionId: {}",
                eventType, tenantId, businessId, transactionId);

        // Validate request according to business rules
        validationService.validateEventConfiguration(request);

        // Using injected mongoTemplate - tenant-aware

        EventConfiguration existingConfig = eventConfigurationRepository
                .findByBusinessIdAndEventType(businessId, eventType, mongoTemplate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event configuration not found for eventType: " + eventType + " and business: " + businessId));

        if (!request.getEventType().equals(eventType) && 
            eventConfigurationRepository.existsByBusinessIdAndEventType(businessId, request.getEventType(), mongoTemplate)) {
            throw new BusinessException("DUPLICATE_EVENT_TYPE", "Event configuration with new eventType '" + request.getEventType() + 
                                      "' already exists for business: " + businessId);
        }

        // Data processor validation is handled through the notification configuration

        eventConfigurationMapper.updateEntity(existingConfig, request);
        existingConfig.setUpdatedAt(LocalDateTime.now());

        EventConfiguration updated = eventConfigurationRepository.save(existingConfig, mongoTemplate);
        
        log.info("Successfully updated event configuration for eventType: {} and business: {}", 
                updated.getEventType(), businessId);
        
        return eventConfigurationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteEventConfiguration(String businessId,
                                       String eventType,
                                       String tenantId,
                                       String transactionId) {
        log.info("Deleting event configuration for eventType: {}, tenant: {}, business: {}, transactionId: {}", 
                eventType, tenantId, businessId, transactionId);

        // Using injected mongoTemplate - tenant-aware

        if (!eventConfigurationRepository.existsByBusinessIdAndEventType(businessId, eventType, mongoTemplate)) {
            throw new ResourceNotFoundException(
                    "Event configuration not found for eventType: " + eventType + " and business: " + businessId);
        }

        long deletedCount = eventConfigurationRepository.deleteByBusinessIdAndEventType(
                businessId, eventType, mongoTemplate);
        
        if (deletedCount == 0) {
            throw new BusinessException("DELETE_FAILED", "Failed to delete event configuration for eventType: " + eventType);
        }

        log.info("Successfully deleted event configuration for eventType: {} and business: {}", 
                eventType, businessId);
    }

    @Override
    public CountResponseDto getEventConfigurationCount(String tenantId, 
                                                  String businessId, 
                                                  Boolean isActive,
                                                  String priority) {
        log.info("Getting event configuration count for tenant: {}, business: {}, isActive: {}, priority: {}", 
                tenantId, businessId, isActive, priority);

        // Using injected mongoTemplate - tenant-aware

        long totalCount = eventConfigurationRepository.countByBusinessId(businessId, mongoTemplate);
        
        Map<String, Long> breakdown = Map.of(
            "ACTIVE", eventConfigurationRepository.countByBusinessIdAndIsActive(businessId, true, mongoTemplate),
            "INACTIVE", eventConfigurationRepository.countByBusinessIdAndIsActive(businessId, false, mongoTemplate),
            "HIGH_PRIORITY", eventConfigurationRepository.countByBusinessIdAndPriority(businessId, EventPriority.HIGH, mongoTemplate),
            "MEDIUM_PRIORITY", eventConfigurationRepository.countByBusinessIdAndPriority(businessId, EventPriority.MEDIUM, mongoTemplate),
            "LOW_PRIORITY", eventConfigurationRepository.countByBusinessIdAndPriority(businessId, EventPriority.LOW, mongoTemplate)
        );

        long filteredCount = totalCount;
        if (isActive != null) {
            filteredCount = eventConfigurationRepository.countByBusinessIdAndIsActive(businessId, isActive, mongoTemplate);
        }
        if (priority != null && !priority.isEmpty()) {
            EventPriority eventPriority = EventPriority.valueOf(priority.toUpperCase());
            filteredCount = eventConfigurationRepository.countByBusinessIdAndPriority(businessId, eventPriority, mongoTemplate);
        }

        return CountResponseDto.builder()
                .data(CountResponseDto.CountData.builder()
                        .totalCount(filteredCount)
                        .build())
                .build();
    }

    @Override
    public EventConfiguration validateEventConfiguration(String businessId, 
                                                       String eventType, 
                                                       String tenantId) {
        log.info("Validating event configuration for eventType: {}, tenant: {}, business: {}", 
                eventType, tenantId, businessId);

        // Using injected mongoTemplate - tenant-aware

        EventConfiguration eventConfiguration = eventConfigurationRepository
                .findByBusinessIdAndEventType(businessId, eventType, mongoTemplate)
                .orElseThrow(() -> new ValidationException(
                        "Event configuration not found for eventType: " + eventType + " and business: " + businessId));

        if (!eventConfiguration.getIsActive()) {
            throw new ValidationException(
                    "Event configuration is inactive for eventType: " + eventType + " and business: " + businessId);
        }

        log.info("Successfully validated event configuration for eventType: {} and business: {}", 
                eventType, businessId);

        return eventConfiguration;
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