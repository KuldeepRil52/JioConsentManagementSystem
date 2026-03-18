package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jio.partnerportal.dto.RolesAccess;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("components")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({
        "componentId",
        "componentName",
        "componentUrl",
        "displayLabel",
        "section",
        "createdAt",
        "updatedAt"
})
public class Component extends AbstractEntity {

    @Id
    @JsonIgnore
    private ObjectId id;
    private String componentId;
    private String componentName;
    private String componentUrl;
    private String displayLabel;
    private String section;

}
