package com.jio.digigov.fides.repository;

import com.jio.digigov.fides.entity.ConsentWithdrawalJob;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConsentWithdrawalJobRepository extends MongoRepository<ConsentWithdrawalJob, String> {
}