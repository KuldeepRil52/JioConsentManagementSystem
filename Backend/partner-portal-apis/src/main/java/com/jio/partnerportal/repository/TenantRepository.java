package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.TenantRegistry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TenantRepository extends MongoRepository<TenantRegistry, Long> {

    /**
     * Finds a tenant by its PAN.
     *
     * @param pan the PAN of the tenant
     * @return an Optional containing the TenantAdmin if found, or empty if not found
     */
    Optional<TenantRegistry> findByPan(String pan);

    Optional<TenantRegistry> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);
    
    Optional<TenantRegistry> findByClientId(String pan);
}
