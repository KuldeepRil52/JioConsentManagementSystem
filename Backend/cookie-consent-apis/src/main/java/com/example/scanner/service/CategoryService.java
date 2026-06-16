package com.example.scanner.service;
import com.example.scanner.config.MultiTenantMongoConfig;
import com.example.scanner.config.TenantContext;
import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.request.AddCookieCategoryRequest;
import com.example.scanner.dto.request.UpdateCookieCategoryRequest;
import com.example.scanner.dto.response.CookieCategoryResponse;
import com.example.scanner.entity.CookieCategory;
import com.example.scanner.exception.CategoryUpdateException;
import com.example.scanner.repository.impl.CategoryRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepositoryImpl categoryRepository;
    private final MultiTenantMongoConfig mongoConfig;

    public CookieCategoryResponse addCategory(AddCookieCategoryRequest request, String tenantId) {
        try {
            // Set tenant context for multi-tenant database connection
            TenantContext.setCurrentTenant(tenantId);
            log.info("Processing add category request");

            // Validate request
            if (request == null) {
                log.error("Add category request is null");
                return CookieCategoryResponse.builder()
                        .success(false)
                        .message("Invalid request")
                        .build();
            }

            // Check if category already exists
            Optional<CookieCategory> existingCookie = categoryRepository.findByCategory(request.getCategory());

            if (existingCookie.isPresent()) {
                log.warn("Category already exists");
                return CookieCategoryResponse.builder()
                        .success(false)
                        .message("Category already exists: " + request.getCategory())
                        .build();
            }

            // Create new cookie category
            CookieCategory cookieCategory = new CookieCategory();
            cookieCategory.setCategoryId(UUID.randomUUID().toString());
            cookieCategory.setCategory(request.getCategory());
            cookieCategory.setDescription(request.getDescription());
            cookieCategory.setDefault(false);
            cookieCategory.setCreatedAt(new Date());
            cookieCategory.setUpdatedAt(new Date());

            // Save to database
            CookieCategory savedCookie = categoryRepository.save(cookieCategory);

            log.info("Successfully added category");

            // Build success response
            return CookieCategoryResponse.builder()
                    .categoryId(savedCookie.getCategoryId())
                    .category(savedCookie.getCategory())
                    .description(savedCookie.getDescription())
                    .isDefault(savedCookie.isDefault())
                    .createdAt(savedCookie.getCreatedAt())
                    .updatedAt(savedCookie.getUpdatedAt())
                    .success(true)
                    .message("Category added successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error adding category");
            return CookieCategoryResponse.builder()
                    .success(false)
                    .message("Failed to add category: " + e.getMessage())
                    .build();
        } finally {
            // Clear tenant context
            TenantContext.clear();
        }
    }

    public CookieCategoryResponse updateCookieCategory(UpdateCookieCategoryRequest request, String tenantId) {
        try {
            TenantContext.setCurrentTenant(tenantId);
            log.info("Processing update category request");

            // Validate request
            if (request == null) {
                log.error("Request is null");
                throw new IllegalArgumentException("Update request cannot be null");
            }

            CookieCategory category = categoryRepository.findByCategory(request.getCategory())
                    .orElseThrow(() -> new CategoryUpdateException(ErrorCodes.CATEGORY_NOT_FOUND,
                            "No category found with name '" + request.getCategory() + "'",
                            "Category '" + request.getCategory() + "' does not exist in tenant: " + tenantId));

            category.setDescription(request.getDescription());
            category.setUpdatedAt(new Date());
            CookieCategory upDatedCookieCategory = categoryRepository.save(category);

            log.info("Successfully updated category");

            return CookieCategoryResponse.builder()
                    .categoryId(upDatedCookieCategory.getCategoryId())
                    .category(upDatedCookieCategory.getCategory())
                    .description(upDatedCookieCategory.getDescription())
                    .isDefault(upDatedCookieCategory.isDefault())
                    .createdAt(upDatedCookieCategory.getCreatedAt())
                    .updatedAt(upDatedCookieCategory.getUpdatedAt())
                    .success(true)
                    .message("Category updated successfully")
                    .build();
        } catch (CategoryUpdateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating category");
            throw new CategoryUpdateException(
                    ErrorCodes.CATEGORY_UPDATE_FAILED,
                    "Failed to update category. Please try again later.",
                    e.getMessage()
            );
        } finally {
            TenantContext.clear();
        }
    }

    public List<CookieCategoryResponse> findAll(String tenantId){
        try {
            TenantContext.setCurrentTenant(tenantId);

            List<CookieCategory> categories = categoryRepository.findAll();
            
            return categories.stream()
                    .map(cat -> CookieCategoryResponse.builder()
                            .categoryId(cat.getCategoryId())
                            .category(cat.getCategory())
                            .description(cat.getDescription())
                            .isDefault(cat.isDefault())
                            .createdAt(cat.getCreatedAt())
                            .updatedAt(cat.getUpdatedAt())
                            .success(true)
                            .message("Category fetched successfully")
                            .build())
                    .toList();
        }catch (Exception e){
            CookieCategoryResponse.builder()
                    .success(false)
                    .message("Failed to fetch all categories: " + e.getMessage())
                    .build();
        }
        return List.of();
    }

    /**
     * Check if a category exists for a given tenant
     * @param category Category name to check
     * @param tenantId Tenant ID
     * @return true if category exists, false otherwise
     */
    public boolean categoryExists(String category, String tenantId) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            Query query = new Query(Criteria.where("category").is(category));
            return tenantMongoTemplate.exists(query, CookieCategory.class);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Get all categories for a tenant (useful for validation)
     */
    public List<String> getAllCategoryNames(String tenantId) {
        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            List<CookieCategory> categories = tenantMongoTemplate.findAll(CookieCategory.class);
            return categories.stream()
                    .map(CookieCategory::getCategory)
                    .collect(Collectors.toList());
        } finally {
            TenantContext.clear();
        }
    }
}