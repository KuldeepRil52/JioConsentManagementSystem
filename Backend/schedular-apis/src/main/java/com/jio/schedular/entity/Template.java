package com.jio.schedular.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.schedular.dto.*;
import com.jio.schedular.enums.TemplateStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Document("templates")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class Template extends AbstractEntity{

    @Id
    private ObjectId id;
    private String templateId;
    private String templateName;
    private String businessId;
    private TemplateStatus status;
    private Multilingual multilingual;
    private UiConfig uiConfig;
    private DocumentMeta documentMeta;
    private List<Preference> preferences;
    private int version;

}
