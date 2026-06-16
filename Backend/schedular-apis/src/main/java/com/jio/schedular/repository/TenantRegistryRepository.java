package com.jio.schedular.repository;

import com.jio.schedular.enums.Status;
import com.jio.schedular.entity.TenantRegistry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantRegistryRepository extends MongoRepository<TenantRegistry, Long> {

    boolean existsByTenantId(String tenantId);

    /**
     * Finds all active tenants
     * 
     * @return List of active tenant registries
     */
    List<TenantRegistry> findByStatus(Status status);
}
