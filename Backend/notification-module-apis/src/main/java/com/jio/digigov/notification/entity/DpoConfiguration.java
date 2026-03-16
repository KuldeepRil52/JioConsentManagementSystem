package com.jio.digigov.notification.entity;

import com.jio.digigov.notification.entity.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entity representing Data Protection Officer (DPO) configuration.
 *
 * <p>This entity stores the contact information for the Data Protection Officer
 * responsible for handling privacy compliance and grievances.</p>
 *
 * <p><b>Hierarchical Scope Levels:</b></p>
 * <ul>
 *   <li><b>BUSINESS:</b> DPO specific to a business (has businessId)</li>
 *   <li><b>TENANT:</b> DPO shared across all businesses in a tenant (no businessId)</li>
 * </ul>
 *
 * <p><b>Lookup Strategy:</b></p>
 * <ol>
 *   <li>First, search for DPO with matching businessId (BUSINESS scope)</li>
 *   <li>If not found, fall back to TENANT scope (no businessId)</li>
 * </ol>
 *
 * <p><b>Usage in Notification System:</b></p>
 * <ul>
 *   <li>DPO receives email notifications for grievance-related events</li>
 *   <li>DPO email address fetched at runtime when processing grievance events</li>
 *   <li>Event configurations specify dataProtectionOfficer.enabled to control notifications</li>
 *   <li>Templates with recipientType=DATA_PROTECTION_OFFICER are used for DPO emails</li>
 * </ul>
 *
 * @since 2.0.0
 * @author DPDP Notification Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dpo_configurations")
public class DpoConfiguration extends BaseEntity {

    /**
     * Unique configuration identifier.
     * Generated UUID for each DPO configuration.
     */
    @Field("configId")
    private String configId;

    /**
     * Business identifier for business-scoped DPO.
     * Null for tenant-scoped DPO (scopeLevel=TENANT).
     */
    @Field("businessId")
    private String businessId;

    /**
     * Scope level of this DPO configuration.
     * Values: "BUSINESS" or "TENANT"
     */
    @Field("scopeLevel")
    private String scopeLevel;

    /**
     * DPO configuration details containing contact information.
     * This embedded object stores all DPO-related contact data.
     *
     * <p>Required fields within configurationJson:</p>
     * <ul>
     *   <li>name - Full name of the DPO</li>
     *   <li>email - Official email address for DPO communications</li>
     *   <li>mobile - Contact phone number (10 digits)</li>
     *   <li>address - Physical office address</li>
     * </ul>
     */
    @Field("configurationJson")
    private ConfigurationJson configurationJson;

    /**
     * Embedded class containing DPO contact information.
     *
     * <p>This class encapsulates all contact details required to reach
     * the Data Protection Officer for privacy-related communications.</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigurationJson {

        /**
         * Full name of the Data Protection Officer.
         * Example: "Jane Doe" or "Dr. Rajesh Kumar"
         */
        @Field("name")
        @NotBlank(message = "DPO name is required")
        @Size(min = 2, max = 100, message = "DPO name must be between 2 and 100 characters")
        private String name;

        /**
         * Official email address for DPO.
         * All grievance-related email notifications will be sent to this address.
         * Must be a valid email format.
         * Example: "dpo@company.com" or "privacy.officer@organization.in"
         */
        @Field("email")
        @NotBlank(message = "DPO email is required")
        @Email(message = "DPO email must be a valid email address")
        private String email;

        /**
         * Contact phone number for DPO.
         * Should be a 10-digit Indian mobile number.
         * Example: "8475487845" or "9876543210"
         */
        @Field("mobile")
        @NotBlank(message = "DPO mobile number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit Indian number")
        private String mobile;

        /**
         * Physical office address of the DPO.
         * Full address where the DPO can be contacted.
         * Example: "5th Floor, ABC Tower, Bandra Kurla Complex, Mumbai - 400051"
         */
        @Field("address")
        @Size(max = 500, message = "Address cannot exceed 500 characters")
        private String address;
    }
}
