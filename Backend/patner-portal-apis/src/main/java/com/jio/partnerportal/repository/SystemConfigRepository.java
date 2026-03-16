package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.SystemConfig;

import java.util.List;
import java.util.Map;

public interface SystemConfigRepository {

    SystemConfig save(SystemConfig config);
    SystemConfig findByConfigId(String businessId);
    List<SystemConfig> findConfigByParams(Map<String, String> param);
    long count();
    boolean existByScopeLevel(String scopeLevel);
    boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId);

}
