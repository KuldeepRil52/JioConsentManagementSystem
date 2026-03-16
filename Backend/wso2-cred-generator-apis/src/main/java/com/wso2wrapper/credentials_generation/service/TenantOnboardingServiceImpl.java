package com.wso2wrapper.credentials_generation.service;

import com.wso2wrapper.credentials_generation.constants.ErrorCodes;
import com.wso2wrapper.credentials_generation.constants.ExternalApiConstants;
import com.wso2wrapper.credentials_generation.dto.OnboardRequestDto;
import com.wso2wrapper.credentials_generation.dto.OnboardResponseDto;
import com.wso2wrapper.credentials_generation.dto.entity.Tenant;
import com.wso2wrapper.credentials_generation.dto.entity.WSO2_BusinessApplication;
import com.wso2wrapper.credentials_generation.dto.response.ApplicationResponse;
import com.wso2wrapper.credentials_generation.dto.response.KeyResponse;
import com.wso2wrapper.credentials_generation.exception.ApiException;
import com.wso2wrapper.credentials_generation.multitenancy.TenantMongoTemplateProvider;
import com.wso2wrapper.credentials_generation.utils.ValidationUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class TenantOnboardingServiceImpl implements TenantOnboardingService {

        private final ExternalApiClient externalApiClient;
        private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

        public TenantOnboardingServiceImpl(
                ExternalApiClient externalApiClient,
                TenantMongoTemplateProvider tenantMongoTemplateProvider) {
                this.externalApiClient = externalApiClient;
                this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        }

        @Override
        public OnboardResponseDto registerTenant(OnboardRequestDto request) {

                // ==================== VALIDATIONS ====================
                ValidationUtils.validateTenantRequest(request);
                String cleanedTenantName = request.getTenantName().trim().replaceAll("\\s+", "");
                request.setTenantName(cleanedTenantName);

                // ==================== TENANT-SPECIFIC DB ====================

                String tenantDbName = "tenant_db_" + request.getTenantId();

                MongoTemplate tenantTemplate = tenantMongoTemplateProvider.getTenantTemplate(request.getTenantId());

                // Check if the tenant database exists by seeing if it has any collections
                boolean dbExists = !tenantTemplate.getDb().listCollectionNames().into(new ArrayList<>()).isEmpty();

                if (!dbExists) {
                        throw new ApiException(
                                HttpStatus.NOT_FOUND,
                                ErrorCodes.ERR_TENANT_DB_NOT_FOUND,
                                tenantDbName
                        );
                }

                ValidationUtils.validateTenantNotAlreadyOnboarded(request, tenantTemplate);

                // ==================== WSO2 REGISTRATION ====================
                externalApiClient.callRegisterApi(request);
                String password = String.format(ExternalApiConstants.DEFAULT_TENANT_PASSWORD_PATTERN, cleanedTenantName);

                Tenant wsTenant = ValidationUtils.createTenantEntity(request, password);
                tenantTemplate.save(wsTenant, ExternalApiConstants.DB_WSO2_TENANTS);

                return OnboardResponseDto.tenantOnly(wsTenant.getTenantUniqueId(), ExternalApiConstants.SUCCESS_TENANT_REGISTERED);
        }
}
