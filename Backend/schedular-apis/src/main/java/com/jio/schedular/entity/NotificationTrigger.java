package com.jio.schedular.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.schedular.dto.CustomerIdentifiers;
import com.jio.schedular.enums.NotificationStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "notification_triggers")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationTrigger extends AbstractEntity {

    @Id
    private ObjectId id;
    private String triggerId;
    private String eventType;
    private String resource;
    private String description;
    private String businessId;
    private CustomerIdentifiers customerIdentifiers;
    private List<String> dataProcessorsIds;
    private NotificationStatus status;
    private Object eventPayload;
    private String httpStatus;
    private String notificationEventId;
    private String errorMessage;

}
