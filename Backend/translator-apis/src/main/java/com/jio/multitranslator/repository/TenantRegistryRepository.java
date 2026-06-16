package com.jio.multitranslator.repository;

import com.jio.multitranslator.entity.TenantRegistry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRegistryRepository extends MongoRepository<TenantRegistry, Long> {

    boolean existsByTenantId(String s);

}
