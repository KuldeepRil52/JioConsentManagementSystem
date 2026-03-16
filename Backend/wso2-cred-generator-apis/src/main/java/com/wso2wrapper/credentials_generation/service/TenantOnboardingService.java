package com.wso2wrapper.credentials_generation.service;


import com.wso2wrapper.credentials_generation.dto.OnboardRequestDto;
import com.wso2wrapper.credentials_generation.dto.OnboardResponseDto;

/**
 * Service interface for registering a tenant and performing initial onboarding.
 */


public interface TenantOnboardingService {

    /**
     * Registers a new tenant and performs full onboarding.
     * This includes:
     *  - Saving the tenant if it doesn't exist
     *  - Generating WSO2 access token
     *  - Creating a new application in WSO2
     *  - Generating keys for the application
     *  - Subscribing to APIs
     *  - Persisting credentials in the database
     * @return OnboardResponseDto containing consumerKey and consumerSecret
     */
    OnboardResponseDto registerTenant(OnboardRequestDto request);
}
