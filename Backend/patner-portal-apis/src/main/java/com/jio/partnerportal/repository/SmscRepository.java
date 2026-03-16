package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.SmscConfig;

import java.util.List;
import java.util.Map;

public interface SmscRepository {
    SmscConfig save(SmscConfig config);

    SmscConfig findByConfigId(String configId);

    List<SmscConfig> findConfigByParams(Map<String, String> searchParams);

    long count();

    boolean existByScopeLevel(String scopeLevel);

    boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId);

}
