package com.jio.partnerportal.service;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.request.UserDashboardThemeRequest;
import com.jio.partnerportal.entity.UserDashboardTheme;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.UserDashboardThemeRepository;
import com.jio.partnerportal.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UserDashboardThemeService {
    
    private UserDashboardThemeRepository userDashboardThemeRepository;

    @Autowired
    public UserDashboardThemeService(UserDashboardThemeRepository userDashboardThemeRepository) {
        this.userDashboardThemeRepository = userDashboardThemeRepository;
    }

    public UserDashboardTheme createTheme(UserDashboardThemeRequest request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Create user dashboard theme";
        
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        
        // Check if theme already exists for this tenant and business
        if (this.userDashboardThemeRepository.existsByTenantIdAndBusinessId(tenantId, businessId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3003);
        }
        
        UserDashboardTheme theme = UserDashboardTheme.builder()
                .themeId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .businessId(businessId)
                .theme(request.getTheme() != null ? request.getTheme() : "")
                .build();
        
        UserDashboardTheme savedTheme = this.userDashboardThemeRepository.save(theme);
        LogUtil.logActivity(req, activity, "Success: Create user dashboard theme successfully");
        return savedTheme;
    }

    public UserDashboardTheme updateTheme(UserDashboardThemeRequest request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Update user dashboard theme";
        
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        
        UserDashboardTheme existingTheme = this.userDashboardThemeRepository.findByTenantIdAndBusinessId(tenantId, businessId);
        if (ObjectUtils.isEmpty(existingTheme)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        
        existingTheme.setTheme(request.getTheme() != null ? request.getTheme() : "");
        UserDashboardTheme updatedTheme = this.userDashboardThemeRepository.save(existingTheme);
        LogUtil.logActivity(req, activity, "Success: Update user dashboard theme successfully");
        return updatedTheme;
    }

    public UserDashboardTheme getTheme(Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Get user dashboard theme";
        
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        
        UserDashboardTheme theme = this.userDashboardThemeRepository.findByTenantIdAndBusinessId(tenantId, businessId);
        if (ObjectUtils.isEmpty(theme)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        
        LogUtil.logActivity(req, activity, "Success: Get user dashboard theme successfully");
        return theme;
    }
}

