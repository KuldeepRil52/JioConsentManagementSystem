package com.jio.digigov.grievance.repository;

import com.jio.digigov.grievance.entity.UserType;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserTypeRepository extends MongoRepository<UserType, String> {
    Optional<UserType> findByUserTypeId(String userTypeId);
    long countBy();
}
