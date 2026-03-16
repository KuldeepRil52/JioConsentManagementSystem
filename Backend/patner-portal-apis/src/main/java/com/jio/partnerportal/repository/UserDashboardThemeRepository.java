package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.UserDashboardTheme;

public interface UserDashboardThemeRepository {
    UserDashboardTheme save(UserDashboardTheme userDashboardTheme);
    
    UserDashboardTheme findByTenantIdAndBusinessId(String tenantId, String businessId);
    
    boolean existsByTenantIdAndBusinessId(String tenantId, String businessId);
}

