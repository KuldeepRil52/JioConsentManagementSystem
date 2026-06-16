package com.wso2wrapper.credentials_generation.utils;

import com.wso2wrapper.credentials_generation.constants.ErrorCodes;
import com.wso2wrapper.credentials_generation.constants.ExternalApiConstants;
import com.wso2wrapper.credentials_generation.dto.OnboardRequestDto;
import com.wso2wrapper.credentials_generation.dto.OnboardResponseDto;
import com.wso2wrapper.credentials_generation.dto.entity.Tenant;
import com.wso2wrapper.credentials_generation.dto.entity.WSO2_BusinessApplication;
import com.wso2wrapper.credentials_generation.dto.entity.WSO2_Data_Processor;
import com.wso2wrapper.credentials_generation.dto.response.ApplicationResponse;
import com.wso2wrapper.credentials_generation.dto.response.KeyResponse;
import com.wso2wrapper.credentials_generation.exception.ApiException;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.data.mongodb.core.query.Query;

import java.util.UUID;

public class ValidationUtils {

    private ValidationUtils() {}

    public static void validateOnboardRequestMinimal(OnboardRequestDto request) {
        if (request == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.ERR_REQUEST_NULL);
        if (request.getTenantId() == null || request.getTenantId().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.ERR_TENANT_ID_REQUIRED);
        if (request.getBusinessId() == null || request.getBusinessId().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.ERR_BUSINESS_ID_REQUIRED);
        if (request.getBusinessName() == null || request.getBusinessName().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.ERR_BUSINESS_NAME_NOT_FOUND);
    }

    public static void validateDataProcessorOnboardRequestMinimal(OnboardRequestDto request) {
        if (request == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.ERR_REQUEST_NULL);
        if (request.getTenantId() == null || request.getTenantId().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.ERR_TENANT_ID_REQUIRED);
        if (request.getDataProcessorId() == null || request.getDataProcessorId().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.ERR_DATA_PROCESSOR_ID_REQUIRED);
        if (request.getDataProcessorName() == null || request.getDataProcessorName().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.ERR_DATA_PROCESSOR_NAME_NOT_FOUND);
    }

    public static void validateTenantRequest(OnboardRequestDto request) {
        if (request == null)
            throw new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.ERR_REQUEST_NULL);
        if (request.getTenantId() == null || request.getTenantId().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.ERR_TENANT_ID_REQUIRED);
        if (request.getTenantName() == null || request.getTenantName().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.ERR_TENANT_NAME_REQUIRED);
    }

    public static void validateTenantNotAlreadyOnboarded(OnboardRequestDto request, MongoTemplate tenantTemplate) {
        // ===== 1. Check if tenantId already exists =====
        long count = tenantTemplate.count(new Query(), Tenant.class, ExternalApiConstants.DB_WSO2_TENANTS);

        if (count > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.ERR_TENANT_ALREADY_EXISTS
            );
        }

        // ===== 2. Check if tenantName already exists for another tenantId
        Query tenantNameQuery = new Query(Criteria.where("tenantName").is(request.getTenantName()));
        Tenant existingTenantByName = tenantTemplate.findOne(tenantNameQuery, Tenant.class, ExternalApiConstants.DB_WSO2_TENANTS);

        if (existingTenantByName != null && !existingTenantByName.getTenantUniqueId().equals(request.getTenantId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.ERR_TENANT_NAME_MISMATCH,
                    request.getTenantName()
            );
        }
    }

    public static String validateAndFetchBusinessName(MongoTemplate tenantTemplate, String businessId) {

        // Check business_applications
        Query businessQuery = new Query(Criteria.where("businessId").is(businessId));
        Document tenantBusiness = tenantTemplate.findOne(businessQuery, Document.class,ExternalApiConstants.DB_BUSINESS_APPLICATIONS);

        if (tenantBusiness == null || tenantBusiness.getString("name") == null || tenantBusiness.getString("name").isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.ERR_BUSINESS_NAME_NOT_FOUND, businessId);
        }

        return tenantBusiness.getString("name");
    }

    public static OnboardResponseDto checkIfAlreadyOnboarded(
            MongoTemplate tenantTemplate, String businessId, String businessName) {

        Query wsQuery = new Query(Criteria.where("businessId").is(businessId));
        WSO2_BusinessApplication existingApp = tenantTemplate.findOne(
                wsQuery,
                WSO2_BusinessApplication.class,
                ExternalApiConstants.DB_WSO2_BUSINESS_APPLICATIONS
        );

        if (existingApp != null) {
            if (!existingApp.getBusinessName().equalsIgnoreCase(businessName)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.ERR_BUSINESS_NAME_UNMATCHED);
            }

            // Return existing OnboardResponseDto
            OnboardResponseDto dto = OnboardResponseDto.businessAlreadyOnboarded(
                    existingApp.getConsumerKey(),
                    existingApp.getConsumerSecret(),
                    existingApp.getBusinessUniqueId()
            );
            dto.setMessage(ExternalApiConstants.INFO_BUSINESS_ALREADY_ONBOARDED);
            return dto;
        }
        return null;
    }

    public static OnboardResponseDto checkIfAlreadyOnboardedDataProcessor(
            MongoTemplate tenantTemplate, String dataProcessorId, String dataProcessorName) {

        // Query WSO2_Data_Processor by dataProcessorId
        Query wsQuery = new Query(Criteria.where("dataProcessorId").is(dataProcessorId));
        WSO2_Data_Processor existingApp = tenantTemplate.findOne(
                wsQuery,
                WSO2_Data_Processor.class,
                ExternalApiConstants.DB_WSO2_DATA_PROCESSOR
        );

        if (existingApp != null) {
            if (!existingApp.getDataProcessorName().equalsIgnoreCase(dataProcessorName)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.ERR_DATA_PROCESSOR_NAME_UNMATCHED);
            }

            OnboardResponseDto dto = OnboardResponseDto.dataProcessorAlreadyOnboarded(
                    existingApp.getConsumerKey(),
                    existingApp.getConsumerSecret(),
                    existingApp.getDataProcessorUniqueId()
            );
            dto.setMessage(ExternalApiConstants.INFO_DATA_PROCESSOR_ALREADY_ONBOARDED);
            return dto;
        }
        return null;
    }


    public static void validateDuplicateBusinessName(MongoTemplate tenantTemplate, OnboardRequestDto request) {
        Query nameQuery = new Query(Criteria.where("businessName").is(request.getBusinessName()));

        boolean exists = tenantTemplate.exists(
                nameQuery,
                WSO2_BusinessApplication.class,
                ExternalApiConstants.DB_WSO2_BUSINESS_APPLICATIONS
        );

        if (exists) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.ERR_BUSINESS_NAME_ALREADY_EXISTS,
                    request.getBusinessName()
            );
        }
    }

    public static void validateDuplicateDataProcessorName(MongoTemplate tenantTemplate, OnboardRequestDto request){
            Query nameQuery = new Query(Criteria.where("dataProcessorName").is(request.getDataProcessorName()));

            boolean exists = tenantTemplate.exists(
                    nameQuery,
                    WSO2_Data_Processor.class,
                    ExternalApiConstants.DB_WSO2_DATA_PROCESSOR
            );

            if (exists) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        ErrorCodes.ERR_DATA_PROCESSOR_NAME_ALREADY_EXISTS,
                        request.getDataProcessorName()
                );
            }
        }

    public static Tenant createTenantEntity(OnboardRequestDto request, String password) {
        Tenant tenant = new Tenant();
        tenant.setTenantUniqueId(generateTenantUniqueId(request.getTenantId()));
        tenant.setTenantName(request.getTenantName());
        tenant.setPassword(password);
        return tenant;
    }

    public static WSO2_BusinessApplication createBusinessApplication(String businessId, String businessName,
                                                                     ApplicationResponse appResponse, KeyResponse keysResponse, String businessUniqueId) {
        WSO2_BusinessApplication wsApp = new WSO2_BusinessApplication();
        wsApp.setBusinessId(businessId);
        wsApp.setBusinessName(businessName);
        wsApp.setBusinessUniqueId(businessUniqueId);
        wsApp.setApplicationId(appResponse.getApplicationId());
        wsApp.setConsumerKey(keysResponse.getConsumerKey());
        wsApp.setConsumerSecret(keysResponse.getConsumerSecret());
        return wsApp;
    }

    public static WSO2_Data_Processor createDataProcessor(String dataProcessorId, String dataProcessorName,
                                                                     ApplicationResponse appResponse, KeyResponse keysResponse, String dataProcessorUniqueId) {
        WSO2_Data_Processor wsAppDP = new WSO2_Data_Processor();
        wsAppDP.setDataProcessorId(dataProcessorId);
        wsAppDP.setDataProcessorName(dataProcessorName);
        wsAppDP.setDataProcessorUniqueId(dataProcessorUniqueId);
        wsAppDP.setApplicationId(appResponse.getApplicationId());
        wsAppDP.setConsumerKey(keysResponse.getConsumerKey());
        wsAppDP.setConsumerSecret(keysResponse.getConsumerSecret());
        return wsAppDP;
    }

    public static String generateTenantUniqueId(String tenantId) {
        // Generate a random UUID
        String randomPart = UUID.randomUUID().toString(); // 36 chars
        // Take prefix from tenantId to reference it
        String prefix = tenantId.length() >= 8 ? tenantId.substring(0, 8) : tenantId;
        // Replace first 8 chars of UUID with tenantId prefix
        String tenantUniqueId = prefix + randomPart.substring(8);
        return tenantUniqueId;
    }

    public static String generateTenantBusinessReference(String tenantId, String businessId) {
        String t = tenantId.length() >= 8 ? tenantId.substring(0, 8) : String.format("%-8s", tenantId).replace(' ', '0');
        String b = businessId.length() >= 4 ? businessId.substring(0, 4) : String.format("%-4s", businessId).replace(' ', '0');

        // Generate random segments
        String rand1 = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        String rand2 = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        String rand3 = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // Combine into UUID-like format
        return String.format("%s-%s-%s-%s-%s", t, b, rand1, rand2, rand3).toLowerCase();
    }


}
