package com.jio.schedular.entity;

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
@Document(collection = "digilocker")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class DigilockerConfig extends AbstractEntity {

    @Id
    private ObjectId id;

    private String businessId;

    private String name;

    @Field("client_id")
    private String clientId;

    @Field("client_secret")
    private String clientSecret;

    @Field("redirect_uri")
    private String redirectUri;

    @Field("code_verifier")
    private String codeVerifier;

    private String status;

}
