package com.jio.schedular.repository;

import com.jio.schedular.entity.DigilockerConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DigilockerConfigRepository extends MongoRepository<DigilockerConfig, String> {

    Optional<DigilockerConfig> findFirstByBusinessIdAndStatus(String businessId, String status);

}
