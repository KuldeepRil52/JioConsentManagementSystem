package com.jio.multitranslator.repository;

import com.jio.multitranslator.dto.request.Config;
import com.jio.multitranslator.entity.TranslateConfig;
import com.jio.multitranslator.exceptions.BodyValidationException;

import java.util.List;

public interface TranslateConfigRepository {

    TranslateConfig save(TranslateConfig translateConfig) throws BodyValidationException;

    TranslateConfig getConfigurationDeatils(String tenantId, String businessId);

    List<TranslateConfig> getAllConfig(String tenantId, String businessId, String provider);

    boolean checkBusinessExists(String businessId);

    boolean checkAlreadyExist(String businessId, String provider);

    TranslateConfig updateConfig(String businessId, String scopeLevel, Config config);

    long getCount(String tenantId);
}
