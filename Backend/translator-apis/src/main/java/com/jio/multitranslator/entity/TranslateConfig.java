package com.jio.multitranslator.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.multitranslator.dto.request.Config;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(value = "multitranslateconfig")
public class TranslateConfig extends AbstractEntity {

    @Id
    private ObjectId id;
    @Indexed(unique = true)
    private String configId;
    @Indexed(unique = true)
    private String businessId;
    private String tenantId;
    private String scopeLevel;
    private Config config;


    public TranslateConfig(TranslateConfig other){

        this.configId = other.configId;
        this.businessId = other.businessId;
        this.tenantId = other.tenantId;
        this.scopeLevel = other.scopeLevel;
        this.config = other.config;
    }
}
