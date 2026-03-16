package com.jio.digigov.fides.entity;

import com.jio.digigov.fides.enumeration.Status;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "system_register")
public class SystemRegister extends BaseEntity {

    @Id
    private String id;

    private String businessId;
    private String systemUniqueId;
    private String systemName;
    private String description;
    private Status status;
    private boolean isDeleted = false;
}
