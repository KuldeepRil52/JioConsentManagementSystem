package com.wso2wrapper.credentials_generation.repository;

import com.wso2wrapper.credentials_generation.dto.entity.Tenant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TenantRepository extends MongoRepository<Tenant, String> {
    Optional<Tenant> findByTenantId(String tenantId);
}