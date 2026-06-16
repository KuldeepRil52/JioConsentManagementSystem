package com.jio.digigov.fides.repository;
import com.jio.digigov.fides.entity.Consent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConsentRepository extends MongoRepository<Consent, String> {

    Optional<Consent> findByConsentId(String consentId);
}
