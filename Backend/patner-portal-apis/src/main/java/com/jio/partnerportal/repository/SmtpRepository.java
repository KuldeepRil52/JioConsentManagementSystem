package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.SmtpConfig;

import java.util.List;
import java.util.Map;

public interface SmtpRepository {

    SmtpConfig save(SmtpConfig config);
    SmtpConfig findByConfigId(String businessId);
    List<SmtpConfig> findConfigByParams(Map<String, String> param);
    long count();
    boolean existByScopeLevel(String scopeLevel);
    boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId);

}
