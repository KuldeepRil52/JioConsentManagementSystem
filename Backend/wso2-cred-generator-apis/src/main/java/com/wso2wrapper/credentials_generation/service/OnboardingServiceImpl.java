package com.wso2wrapper.credentials_generation.service;

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
import com.wso2wrapper.credentials_generation.multitenancy.TenantMongoTemplateProvider;
import com.wso2wrapper.credentials_generation.utils.ValidationUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class OnboardingServiceImpl implements OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingServiceImpl.class);

    private final ExternalApiClient externalApiClient;
    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    public OnboardingServiceImpl(ExternalApiClient externalApiClient,
                                 TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.externalApiClient = externalApiClient;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    private String createLog(String username, String activity, String result) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        String sourceIp = "Unknown";
        try {
            sourceIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
        }

        return String.format(
                "Timestamp: %s | SourceIP: %s | Username: %s | Activity: %s | Result: %s",
                timestamp,
                sourceIp,
                username != null ? username : "system",
                activity,
                result
        );
    }

    @Override
    public OnboardResponseDto handleOnboarding(OnboardRequestDto request) {

        final String activity = "Business Application Onboarding";
        try {
        // ==================== 1. Validate minimal request ====================
        ValidationUtils.validateOnboardRequestMinimal(request);

        // ==================== 2. Switch to tenant-specific DB ====================
        MongoTemplate tenantTemplate = tenantMongoTemplateProvider.getTenantTemplate(request.getTenantId());

        // ==================== 3. Check if tenant exists in tenant DB ====================
        Tenant tenant = tenantTemplate.findOne(new Query(), Tenant.class, "wso2_tenants");
        if (tenant == null) {
            log.error(createLog(request.getBusinessName(), activity, "Failed - Tenant not found"));
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.ERR_TENANT_NOT_FOUND);
        }

        String tenantName = tenant.getTenantName();
        String password = tenant.getPassword();


        // ==================== 4. Check if already onboarded in wso2_business_applications ====================
            OnboardResponseDto existing = ValidationUtils.checkIfAlreadyOnboarded(
                    tenantTemplate, request.getBusinessId(), request.getBusinessName()
            );
            if (existing != null) {
                log.info(createLog(request.getBusinessName(), activity, "Already Onboarded - Returning existing details"));
                return existing;
            }

        ValidationUtils.validateDuplicateBusinessName(tenantTemplate, request);

        // ==================== 5. Generate business unique ID ====================
        String businessUniqueId = ValidationUtils.generateTenantBusinessReference(request.getTenantId(), request.getBusinessId());

        // ==================== 6. Generate WSO2 token ====================
        String accessToken = externalApiClient.callTokenApi(tenantName, password);

        // ==================== 7. Create application in WSO2 ====================
        ApplicationResponse appResponse = externalApiClient.createApplication(
                request.getTenantId(), accessToken, request.getBusinessName()
        );
        // ==================== 8. Generate keys ====================
        KeyResponse keysResponse = externalApiClient.generateKeys(
                appResponse.getApplicationId(), request.getTenantId(), accessToken
        );

        // ==================== 9.WSO2 business application ====================
        WSO2_BusinessApplication wsApp = ValidationUtils.createBusinessApplication(
                request.getBusinessId(), request.getBusinessName(), appResponse, keysResponse, businessUniqueId
        );

        tenantTemplate.save(wsApp, ExternalApiConstants.DB_WSO2_BUSINESS_APPLICATIONS);

        // ==================== 10. Subscribe to APIs ====================
        externalApiClient.subscribeMultipleApis(wsApp, accessToken, tenantTemplate);
        log.info(createLog(request.getBusinessName(), activity, "Success"));

        // ==================== 11. Return response ====================
        return OnboardResponseDto.tenantWithBusinessOnboard(
                keysResponse.getConsumerKey(),
                keysResponse.getConsumerSecret(),
                businessUniqueId
        );
    }catch (Exception ex) {
        log.error(createLog(request.getBusinessName(), activity, "Failed - " + ex.getMessage()));
        throw ex;
    }
}

    @Override
    public OnboardResponseDto handleDataProcessorOnboarding(OnboardRequestDto request) {
        final String activity = "Data Processor Onboarding";

        try {

            // ==================== 1. Validate minimal request ====================
            ValidationUtils.validateDataProcessorOnboardRequestMinimal(request);

            // ==================== 2. Switch to tenant-specific DB ====================
            MongoTemplate tenantTemplate = tenantMongoTemplateProvider.getTenantTemplate(request.getTenantId());

            // ==================== 3. Check if tenant exists in tenant DB ====================
            Tenant tenant = tenantTemplate.findOne(new Query(), Tenant.class, "wso2_tenants");
            if (tenant == null) {
                log.error(createLog(request.getDataProcessorName(), activity, "Failed - Tenant not found"));
                throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.ERR_TENANT_NOT_FOUND);
            }

            String tenantName = tenant.getTenantName();
            String password = tenant.getPassword();

            // ==================== 5. Check if already onboarded in wso2 data processor ====================
            OnboardResponseDto existing = ValidationUtils.checkIfAlreadyOnboardedDataProcessor(
                    tenantTemplate, request.getDataProcessorId(), request.getDataProcessorName()
            );
            if (existing != null) {
                log.info(createLog(request.getDataProcessorName(), activity, "Already Onboarded - Returning existing details"));
                return existing;
            }

            // ==================== 5.5. Check if name already exists ====================
            ValidationUtils.validateDuplicateDataProcessorName(tenantTemplate, request);

            // ==================== 6. Generate data processor unique ID ====================
            String dataProcessorUniqueId = ValidationUtils.generateTenantBusinessReference(request.getTenantId(), request.getDataProcessorId());

            // ==================== 7. Generate WSO2 token ====================
            String accessToken = externalApiClient.callTokenApi(tenantName, password);

            // ==================== 8. Create application in WSO2 ====================
            ApplicationResponse appResponse = externalApiClient.createApplication(
                    request.getTenantId(), accessToken, request.getDataProcessorName()
            );

            // ==================== 9. Generate keys ====================
            KeyResponse keysResponse = externalApiClient.generateKeys(
                    appResponse.getApplicationId(), request.getTenantId(), accessToken
            );

            // ==================== 10.WSO2 business application ====================
            WSO2_Data_Processor wsAppDP = ValidationUtils.createDataProcessor(
                    request.getDataProcessorId(), request.getDataProcessorName(), appResponse, keysResponse, dataProcessorUniqueId
            );

            tenantTemplate.save(wsAppDP, ExternalApiConstants.DB_WSO2_DATA_PROCESSOR);

            // ==================== 11. Subscribe to APIs ====================
            externalApiClient.subscribeMultipleApis(wsAppDP, accessToken, tenantTemplate);

            log.info(createLog(request.getDataProcessorName(), activity, "Success"));

            // ==================== 12. Return response ====================
            return OnboardResponseDto.tenantWithDataProcessorOnboard(
                    keysResponse.getConsumerKey(),
                    keysResponse.getConsumerSecret(),
                    dataProcessorUniqueId
            );
        } catch (Exception ex) {
            log.error(createLog(request.getDataProcessorName(), activity, "Failed - " + ex.getMessage()));
            throw ex;
        }
    }
}
