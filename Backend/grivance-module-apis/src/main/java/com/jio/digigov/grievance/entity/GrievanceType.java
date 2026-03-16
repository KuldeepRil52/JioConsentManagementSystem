package com.jio.digigov.grievance.entity;

import com.jio.digigov.grievance.enumeration.ScopeLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "grievance_types")
public class GrievanceType extends BaseEntity {

    @Indexed(unique = true)
    private String grievanceTypeId = UUID.randomUUID().toString();
    private String businessId;
    private String grievanceType;
    private List<String> grievanceItem;
    private ScopeLevel scope;
    private String description;
}
