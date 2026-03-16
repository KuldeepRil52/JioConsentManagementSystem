package com.jio.digigov.grievance.entity;

import com.jio.digigov.grievance.enumeration.ScopeLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_types")
public class UserType extends BaseEntity {

    @Indexed(unique = true)
    private String userTypeId = UUID.randomUUID().toString();

    @Indexed(unique = true)
    private String name;

    private String businessId;

    private ScopeLevel scope;

    private String description;
}
