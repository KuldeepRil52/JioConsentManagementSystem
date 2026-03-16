package com.jio.digigov.grievance.repository;

import com.jio.digigov.grievance.entity.Grievance;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface GrievanceRepository extends MongoRepository<Grievance, String> {
    Optional<Grievance> findByGrievanceId(String grievanceId);
    long countBy();
}
