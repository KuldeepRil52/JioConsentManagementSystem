package com.jio.digigov.grievance.entity;

import com.jio.digigov.grievance.enumeration.ScopeLevel;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_details")

public class UserDetail  extends  BaseEntity {

    @Indexed(unique = true)
    private String userDetailId = UUID.randomUUID().toString();

    @Indexed(unique = true)
    private String name;

    private String businessId;

    private ScopeLevel scope;

    private String description;
}
