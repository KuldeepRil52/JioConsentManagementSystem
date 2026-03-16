package com.jio.digigov.notification.service.event;

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
import com.jio.digigov.notification.service.TenantService;
import com.jio.digigov.notification.service.event.impl.DataProcessorServiceImpl;
import com.jio.digigov.notification.test.BaseConsumerUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Consumer DataProcessorServiceImpl
 *
 * Tests cover:
 * - CRUD operations
 * - Validation scenarios
 * - Error handling
 * - Business logic
 * - Consumer-specific behavior
 */
@ExtendWith(MockitoExtension.class)
class DataProcessorServiceImplTest extends BaseConsumerUnitTest {

    @Mock
    private DataProcessorRepository dataProcessorRepository;

    @Mock
    private DataProcessorMapper dataProcessorMapper;

    @Mock
    private TenantService tenantService;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private DataProcessorServiceImpl dataProcessorService;

    private CreateDataProcessorRequestDto createRequest;
    private DataProcessor dataProcessor;
    private DataProcessorResponseDto responseDto;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        createRequest = CreateDataProcessorRequestDto.builder()
                .dataProcessorId("CONS-DP001")
                .dataProcessorName("Consumer Test Data Processor")
                .details("Consumer test data processor for unit testing")
                .callbackUrl("https://consumer.api.example.com/callback")
                .attachment("consumer-test-attachment.pdf")
                .vendorRiskDocument("consumer-risk-doc.pdf")
                .scopeType("BUSINESS")
                .status("ACTIVE")
                .build();

        dataProcessor = DataProcessor.builder()
                .id("cons-obj-id-123")
                .dataProcessorId("CONS-DP001")
                .dataProcessorName("Consumer Test Data Processor")
                .businessId(TEST_BUSINESS_ID)
                .details("Consumer test data processor for unit testing")
                .callbackUrl("https://consumer.api.example.com/callback")
                .status(DataProcessorStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        responseDto = DataProcessorResponseDto.builder()
                .dataProcessorId("CONS-DP001")
                .dataProcessorName("Consumer Test Data Processor")
                .businessId(TEST_BUSINESS_ID)
                .details("Consumer test data processor for unit testing")
                .callbackUrl("https://consumer.api.example.com/callback")
                .status("ACTIVE")
                .build();
    }

    @Test
    void createDataProcessor_Success_ConsumerModule() {
        // Given
        when(dataProcessorRepository.existsByDataProcessorIdAndBusinessId(anyString(), anyString(), any(MongoTemplate.class)))
                .thenReturn(false);
        when(dataProcessorMapper.toEntity(any(CreateDataProcessorRequestDto.class), anyString()))
                .thenReturn(dataProcessor);
        when(dataProcessorRepository.save(any(DataProcessor.class), any(MongoTemplate.class)))
                .thenReturn(dataProcessor);
        when(dataProcessorMapper.toResponse(any(DataProcessor.class)))
                .thenReturn(responseDto);

        // When
        DataProcessorResponseDto result = dataProcessorService.createDataProcessor(
                createRequest, TEST_TENANT_ID, TEST_BUSINESS_ID, TEST_REQUEST_ID);

        // Then
        assertNotNull(result);
        assertEquals("CONS-DP001", result.getDataProcessorId());
        assertEquals("Consumer Test Data Processor", result.getDataProcessorName());
        assertEquals(TEST_BUSINESS_ID, result.getBusinessId());

        verify(dataProcessorRepository).existsByDataProcessorIdAndBusinessId(
                "CONS-DP001", TEST_BUSINESS_ID, mongoTemplate);
        verify(dataProcessorRepository).save(dataProcessor, mongoTemplate);
        verify(dataProcessorMapper).toResponse(dataProcessor);
    }

    @Test
    void getAllDataProcessors_Success_ConsumerModule() {
        // Given
        Page<DataProcessor> page = new PageImpl<>(Arrays.asList(dataProcessor));
        when(dataProcessorRepository.findByBusinessId(anyString(), any(Pageable.class), any(MongoTemplate.class)))
                .thenReturn(page);
        when(dataProcessorMapper.toResponse(any(DataProcessor.class)))
                .thenReturn(responseDto);

        // When
        PagedResponseDto<DataProcessorResponseDto> result = dataProcessorService.getAllDataProcessors(
                TEST_TENANT_ID, TEST_BUSINESS_ID, null, 0, 10, "createdAt:desc");

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("CONS-DP001", result.getData().get(0).getDataProcessorId());

        verify(dataProcessorRepository).findByBusinessId(eq(TEST_BUSINESS_ID), any(Pageable.class), eq(mongoTemplate));
    }

    @Test
    void validateDataProcessorIds_Success_ConsumerModule() {
        // Given
        List<String> ids = Arrays.asList("CONS-DP001", "CONS-DP002");
        List<DataProcessor> processors = Arrays.asList(dataProcessor);

        // Create second data processor for complete validation
        DataProcessor secondProcessor = DataProcessor.builder()
                .dataProcessorId("CONS-DP002")
                .dataProcessorName("Second Consumer Processor")
                .businessId(TEST_BUSINESS_ID)
                .status(DataProcessorStatus.ACTIVE)
                .build();

        List<DataProcessor> completeList = Arrays.asList(dataProcessor, secondProcessor);

        when(dataProcessorRepository.findActiveByDataProcessorIdsAndBusinessId(anyList(), anyString(), any(MongoTemplate.class)))
                .thenReturn(completeList);

        // When
        List<DataProcessor> result = dataProcessorService.validateDataProcessorIds(ids, TEST_TENANT_ID, TEST_BUSINESS_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(dataProcessorRepository).findActiveByDataProcessorIdsAndBusinessId(ids, TEST_BUSINESS_ID, mongoTemplate);
    }

    @Test
    void deleteDataProcessor_Success_ConsumerModule() {
        // Given
        when(dataProcessorRepository.existsByDataProcessorIdAndBusinessId(anyString(), anyString(), any(MongoTemplate.class)))
                .thenReturn(true);
        when(dataProcessorRepository.deleteByDataProcessorIdAndBusinessId(anyString(), anyString(), any(MongoTemplate.class)))
                .thenReturn(1L);

        // When
        assertDoesNotThrow(() -> dataProcessorService.deleteDataProcessor(
                "CONS-DP001", TEST_TENANT_ID, TEST_BUSINESS_ID, TEST_REQUEST_ID));

        // Then
        verify(dataProcessorRepository).existsByDataProcessorIdAndBusinessId("CONS-DP001", TEST_BUSINESS_ID, mongoTemplate);
        verify(dataProcessorRepository).deleteByDataProcessorIdAndBusinessId("CONS-DP001", TEST_BUSINESS_ID, mongoTemplate);
    }

    @Test
    void getDataProcessorCount_Success_ConsumerModule() {
        // Given
        when(dataProcessorRepository.countByBusinessId(anyString(), any(MongoTemplate.class)))
                .thenReturn(3L);
        when(dataProcessorRepository.countByBusinessIdAndStatus(anyString(), eq(DataProcessorStatus.ACTIVE), any(MongoTemplate.class)))
                .thenReturn(2L);
        when(dataProcessorRepository.countByBusinessIdAndStatus(anyString(), eq(DataProcessorStatus.INACTIVE), any(MongoTemplate.class)))
                .thenReturn(1L);

        // When
        CountResponseDto result = dataProcessorService.getDataProcessorCount(TEST_TENANT_ID, TEST_BUSINESS_ID, null);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(3L, result.getData().getTotalCount());

        verify(dataProcessorRepository).countByBusinessId(TEST_BUSINESS_ID, mongoTemplate);
        verify(dataProcessorRepository).countByBusinessIdAndStatus(TEST_BUSINESS_ID, DataProcessorStatus.ACTIVE, mongoTemplate);
        verify(dataProcessorRepository).countByBusinessIdAndStatus(TEST_BUSINESS_ID, DataProcessorStatus.INACTIVE, mongoTemplate);
    }

    @Test
    void createDataProcessor_ThrowsException_WhenDuplicate_ConsumerModule() {
        // Given
        when(dataProcessorRepository.existsByDataProcessorIdAndBusinessId(anyString(), anyString(), any(MongoTemplate.class)))
                .thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> dataProcessorService.createDataProcessor(createRequest, TEST_TENANT_ID, TEST_BUSINESS_ID, TEST_REQUEST_ID));

        assertEquals("DUPLICATE_DATA_PROCESSOR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("already exists"));
        verify(dataProcessorRepository, never()).save(any(), any());
    }

    @Test
    void getDataProcessorById_ThrowsException_WhenNotFound_ConsumerModule() {
        // Given
        when(dataProcessorRepository.findByDataProcessorIdAndBusinessId(anyString(), anyString(), any(MongoTemplate.class)))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> dataProcessorService.getDataProcessorById("CONS-DP001", TEST_TENANT_ID, TEST_BUSINESS_ID));

        assertTrue(exception.getMessage().contains("Data processor not found"));
        verify(dataProcessorMapper, never()).toResponse(any());
    }
}