package com.jio.consent.repository;

import com.jio.consent.entity.DigilockerConfig;

import java.util.Optional;

public interface DigilockerConfigRepository {

    Optional<DigilockerConfig> findFirstByBusinessIdAndStatus(String businessId, String status);

}
