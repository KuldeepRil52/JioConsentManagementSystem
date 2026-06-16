package com.jio.digigov.grievance.repository;

import com.jio.digigov.grievance.entity.GrievanceType;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface GrievanceTypeRepository extends MongoRepository<GrievanceType, String> {
    Optional<GrievanceType> findByGrievanceTypeId(String grievanceTypeId);
    long countBy();
}
