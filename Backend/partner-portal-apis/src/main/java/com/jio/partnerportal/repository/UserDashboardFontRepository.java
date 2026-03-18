package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.UserDashboardFont;

public interface UserDashboardFontRepository {
    UserDashboardFont save(UserDashboardFont userDashboardFont);
    
    UserDashboardFont findByTenantIdAndBusinessId(String tenantId, String businessId);
    
    boolean existsByTenantIdAndBusinessId(String tenantId, String businessId);
}

