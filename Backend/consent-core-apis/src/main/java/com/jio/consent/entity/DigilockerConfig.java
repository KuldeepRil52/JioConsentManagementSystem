package com.jio.consent.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Document(collection = "digilocker_credentials")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class DigilockerConfig extends AbstractEntity {

    @Id
    private ObjectId id;

    private String businessId;

    private String name;

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private String codeVerifier;

    private String status;

}
