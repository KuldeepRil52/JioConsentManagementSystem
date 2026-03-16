package com.jio.consent.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.LANGUAGE;
import com.jio.consent.dto.PreferenceStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@org.springframework.data.mongodb.core.mapping.Document("consent_meta")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ConsentMeta extends AbstractEntity {

    @Id
    private ObjectId id;
    
    @Indexed(unique = true, name = "consentMetaId")
    private String consentMetaId;
    
    private LANGUAGE languagePreference;
    
    private Map<String, PreferenceStatus> preferencesStatus;
    
    private Boolean isParentalConsent;
    
    private String relation;
    
    private String consentHandleId;
    
    private String secId;
    
    private Map<String, Object> additionalInfo;

}

