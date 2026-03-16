package com.wso2wrapper.credentials_generation.service;

import com.wso2wrapper.credentials_generation.dto.OnboardRequestDto;
import com.wso2wrapper.credentials_generation.dto.OnboardResponseDto;
import org.springframework.stereotype.Service;

@Service

    /**
     * Handles the onboarding process: validates request, checks existing applications,
     * calls internal APIs, generates keys, saves credentials, and returns client info.
     * @param request OnboardRequestDto containing username, tenantId, businessId
     * @return OnboardResponseDto containing clientId and clientSecret
     */

    public interface OnboardingService {
        OnboardResponseDto handleOnboarding(OnboardRequestDto request);
        OnboardResponseDto handleDataProcessorOnboarding(OnboardRequestDto request);
    }


