package com.example.scanner.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.scanner.entity.CookieCategory;


@Repository
public interface CategoryRepository extends MongoRepository<CookieCategory, UUID> {

    Optional<CookieCategory> findByCategory(String category);

    List<CookieCategory> findAll();

    boolean existsByCategory(String category);
}