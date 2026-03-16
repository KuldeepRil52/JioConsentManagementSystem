package com.example.scanner.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Document(collection = "cookie_category_master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieCategory {

    @Id
    @JsonProperty("categoryId")
    private String categoryId;

    @JsonProperty("category")
    private String category;

    @JsonProperty("description")
    private String description;

    @JsonProperty("isDefault")
    private boolean isDefault;

    @JsonProperty("createdAt")
    private Date createdAt;

    @JsonProperty("updatedAt")
    private Date updatedAt;

    public CookieCategory(String category, String description, boolean isDefault) {
        this.categoryId = UUID.randomUUID().toString();
        this.category = category;
        this.description = description;
        this.isDefault = isDefault;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
}