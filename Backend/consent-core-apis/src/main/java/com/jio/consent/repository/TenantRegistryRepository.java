package com.jio.consent.repository;

import com.jio.consent.entity.TenantRegistry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRegistryRepository extends MongoRepository<TenantRegistry, Long> {

    boolean existsByTenantId(String tenantId);

}
