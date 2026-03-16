package com.jio.schedular.repository;

import com.jio.schedular.entity.ParentalKyc;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParentalKycRepository extends MongoRepository<ParentalKyc, ObjectId> {

    Optional<ParentalKyc> findByKycReferenceId(String kycReferenceId);

}
