package com.example.scanner.repository.impl;

import com.example.scanner.entity.CookieCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CategoryRepositoryImpl {

    private final MongoTemplate mongoTemplate;

    public CookieCategory save(CookieCategory cookieCategory) {
        log.debug("Saving category: {}", cookieCategory.getCategory());
        return mongoTemplate.save(cookieCategory, "cookie_category_master");
    }

    public Optional<CookieCategory> findByCategory(String category) {
        log.debug("Finding category by category name: {}", category);
        Query query = new Query(Criteria.where("category").is(category));
        CookieCategory cookieCategory = mongoTemplate.findOne(query, CookieCategory.class, "cookie_category_master");
        return Optional.ofNullable(cookieCategory);
    }

    public List<CookieCategory> findAll() {
        log.debug("Finding category:");
        return mongoTemplate.find(new Query(), CookieCategory.class);
    }

    public CookieCategory update(CookieCategory cookieCategory) {
        log.debug("Updating category: {}", cookieCategory.getCategoryId());
        return mongoTemplate.save(cookieCategory, "cookie_category_master");
    }
}