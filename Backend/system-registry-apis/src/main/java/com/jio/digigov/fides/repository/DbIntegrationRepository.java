package com.jio.digigov.fides.repository;

import com.jio.digigov.fides.entity.DbIntegration;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DbIntegrationRepository extends MongoRepository<DbIntegration, String> {

    Optional<DbIntegration> findByIntegrationIdAndIsDeletedFalse(String integrationId);

    List<DbIntegration> findAllByIsDeletedFalse();

    long countByStatusAndIsDeletedFalse(String status);

    Optional<DbIntegration> findBySystemId(String systemId);
}