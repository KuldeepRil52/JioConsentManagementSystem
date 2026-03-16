package com.example.scanner.repository;

import com.example.scanner.entity.ConsentTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentTemplateRepository extends MongoRepository<ConsentTemplate, String> {

}