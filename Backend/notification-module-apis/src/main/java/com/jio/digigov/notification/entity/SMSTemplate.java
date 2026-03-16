package com.jio.digigov.notification.entity;

import com.jio.digigov.notification.enums.ComponentType;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.enums.TemplateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * SMS Template Entity
 * Stored in tenant-specific database: tenant_db_{tenantId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "sms_templates")
@CompoundIndexes({
    @CompoundIndex(name = "business_scope_type_idx",
                  def = "{'business_id': 1, 'scope_level': 1, 'type': 1}"),
    @CompoundIndex(name = "business_status_idx",
                  def = "{'business_id': 1, 'status': 1}")
})
public class SMSTemplate extends BaseEntity {
    
    // DigiGov returned templateId (primary identifier)
    @Field("template_id")
    @Indexed(unique = true)
    @NotBlank(message = "Template ID is required")
    private String templateId;
    
    @Field("business_id")
    @NotBlank(message = "Business ID is required")
    private String businessId;
    
    @Field("scope_level")
    @NotNull(message = "Scope level is required")
    private ScopeLevel scopeLevel;
    
    @Field("type")
    @NotNull(message = "Notification type is required")
    private NotificationType type;
    
    @Field("component_type")
    private ComponentType componentType;
    
    @Field("event_type")
    private String eventType;
    
    // SMS Template fields for DigiGov
    @Field("dlt_entity_id")
    @NotBlank(message = "DLT Entity ID is required")
    private String dltEntityId;
    
    @Field("dlt_template_id")
    @NotBlank(message = "DLT Template ID is required")
    private String dltTemplateId;
    
    @Field("from")
    @NotBlank(message = "From field is required")
    private String from; // CLI (Sender ID)
    
    @Field("opr_countries")
    @NotEmpty(message = "Operator countries are required")
    private List<String> oprCountries;
    
    @Field("template")
    @NotBlank(message = "Template content is required")
    private String template;
    
    @Field("template_details")
    @NotBlank(message = "Template details are required")
    private String templateDetails;
    
    @Field("white_listed_number")
    @NotEmpty(message = "Whitelisted numbers are required")
    private List<String> whiteListedNumber;
    
    @Field("max_daily_limit")
    private String maxDailyLimit;
    
    @Field("status")
    @NotNull(message = "Template status is required")
    @Builder.Default
    private TemplateStatus status = TemplateStatus.PENDING;
}