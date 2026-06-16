package com.example.scanner.repository;

import com.example.scanner.entity.CookieConsent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConsentRepository extends MongoRepository<CookieConsent, UUID>, ConsentRepositoryCustom {
}