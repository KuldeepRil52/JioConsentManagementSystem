package com.jio.multitranslator.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(value = "translateToken")
public class TranslateToken {

    @Id
    private ObjectId id;
    @Indexed(unique = true)
    private String businessId;
    @Indexed
    private String sourceLanguage;
    @Indexed
    private String targetLanguage;
    private String token;
    private String pipelineId;
    private String serviceId;
    private String callbackUrl;

    public TranslateToken(TranslateToken other){
        this.businessId = other.businessId;
        this.sourceLanguage = other.sourceLanguage;
        this.targetLanguage = other.targetLanguage;
        this.token = other.token;
        this.pipelineId = other.pipelineId;
        this.serviceId = other.serviceId;
        this.callbackUrl = other.callbackUrl;
    }

}
