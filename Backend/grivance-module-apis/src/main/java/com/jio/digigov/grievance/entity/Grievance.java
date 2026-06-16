package com.jio.digigov.grievance.entity;

import com.jio.digigov.grievance.dto.DocumentMeta;
import com.jio.digigov.grievance.dto.StatusTimeline;
import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * Represents a grievance record with feedback and history tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "grievances")
public class Grievance extends BaseEntity {

    @Indexed(unique = true)
    private String grievanceId = UUID.randomUUID().toString();

    private String grievanceTemplateId;      // Template reference
    private String userType;                 // Citizen / Department / Vendor etc.
    private String grievanceType;            // Type (from GrievanceType)
    private String grievanceDetail;          // Sub-type or item
    private String businessId;               // Tenant Business ID
    private Map<String, Object> userDetails; // Dynamic user fields

    /** User's input while creating grievance */
    private String grievanceDescription;

    /** Attachments provided at creation */
    @Builder.Default
    private List<DocumentMeta> supportingDocs = new ArrayList<>();

    /** Current status of grievance */
    @Builder.Default
    private GrievanceStatus status = GrievanceStatus.NEW;

    /** Remarks provided by officers during updates */
    @Builder.Default
    private List<StatusTimeline> resolutionRemarks = new ArrayList<>();

    /** Feedback rating given by user (0 initially, can be updated to 1–5) */
    @Builder.Default
    private Integer feedback = 0;

    /**
     *  History of grievance updates — status changes, feedback, remarks, etc.
     * Each entry contains key details of the event.
     */
    @Builder.Default
    private List<Map<String, Object>> history = new ArrayList<>();

    private String grievanceJwtToken;
}