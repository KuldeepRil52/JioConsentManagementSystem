package com.jio.partnerportal.service;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.request.UserDashboardFontRequest;
import com.jio.partnerportal.dto.request.UserDashboardThemeRequest;
import com.jio.partnerportal.entity.UserDashboardFont;
import com.jio.partnerportal.entity.UserDashboardTheme;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.UserDashboardFontRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Validation;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UserDashboardFontService {
	
	@Autowired
	Validation validation;
    
    private UserDashboardFontRepository userDashboardFontRepository;

    @Autowired
    public UserDashboardFontService(UserDashboardFontRepository userDashboardFontRepository, LegalEntityService legalEntityService) {
        this.userDashboardFontRepository = userDashboardFontRepository;
    }

    public UserDashboardFont createFont(UserDashboardFontRequest request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException, BodyValidationException {
        String activity = "Create user dashboard font";
        
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        validation.validateBusinessIdHeader(headers);
        Validation.validateUploadFontRequest(request);
        // Check if theme already exists for this tenant and business
        if (this.userDashboardFontRepository.existsByTenantIdAndBusinessId(tenantId, businessId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3003);
        }
        
        UserDashboardFont fontEntity = UserDashboardFont.builder()
                .fontId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .businessId(businessId)
                .typographySettings(request.getTypographySettings())
                .build();
        
        UserDashboardFont savedFont = this.userDashboardFontRepository.save(fontEntity);
        LogUtil.logActivity(req, activity, "Success: Uploaded user dashboard font successfully");
        return savedFont;
    }

    public UserDashboardFont updateFont(UserDashboardFontRequest request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException, BodyValidationException {
        String activity = "Update user dashboard fonts";
        
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        validation.validateBusinessIdHeader(headers);
        Validation.validateUploadFontRequest(request);
        
        UserDashboardFont existingFont = this.userDashboardFontRepository.findByTenantIdAndBusinessId(tenantId, businessId);
        if (ObjectUtils.isEmpty(existingFont)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        
        existingFont.setTypographySettings(request.getTypographySettings());
        UserDashboardFont updatedFont = this.userDashboardFontRepository.save(existingFont);
        LogUtil.logActivity(req, activity, "Success: Update user dashboard font successfully");
        return updatedFont;
    }

    public UserDashboardFont getFont(Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException, BodyValidationException {
        String activity = "Get user dashboard fonts";
        
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        
        validation.validateBusinessIdHeader(headers);
        UserDashboardFont font = this.userDashboardFontRepository.findByTenantIdAndBusinessId(tenantId, businessId);
        if (ObjectUtils.isEmpty(font)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        
        LogUtil.logActivity(req, activity, "Success: Get user dashboard font successfully");
        return font;
    }
}

