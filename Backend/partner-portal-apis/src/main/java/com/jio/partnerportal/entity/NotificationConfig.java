package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.NotificationDetails;
import com.jio.partnerportal.dto.ProviderType;
import com.jio.partnerportal.dto.SmtpDetails;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "notification_configurations")
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationConfig extends AbstractEntity {

    @Id
    private ObjectId id;
    private String configId;
    @Indexed(unique = true)
    private String businessId;
    private String scopeLevel;
    private ProviderType providerType;
    private NotificationDetails configurationJson;
    private SmtpDetails smtpDetails;

}
