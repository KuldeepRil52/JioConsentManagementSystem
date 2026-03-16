package com.jio.digigov.fides.repository;

import com.jio.digigov.fides.entity.SystemRegister;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SystemRegisterRepository extends MongoRepository<SystemRegister, String> {

    Optional<SystemRegister> findByBusinessId(String businessId);
}