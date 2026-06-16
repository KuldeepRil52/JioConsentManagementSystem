package com.jio.digigov.grievance.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.grievance.dto.DocumentMeta;
import com.jio.digigov.grievance.dto.StatusTimeline;
import com.jio.digigov.grievance.dto.SupportingDoc;
import com.jio.digigov.grievance.dto.request.GrievanceCreateRequest;
import com.jio.digigov.grievance.dto.request.GrievanceUpdateRequest;
import com.jio.digigov.grievance.dto.response.GrievanceDetailResponse;
import com.jio.digigov.grievance.dto.response.GrievanceListResponse;
import com.jio.digigov.grievance.dto.response.GrievanceResponse;
import com.jio.digigov.grievance.entity.Grievance;
import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import com.jio.digigov.grievance.enumeration.GrievanceUpdateStatus;
import com.jio.digigov.grievance.exception.InvalidRequestException;

import java.time.LocalDateTime;
import java.util.*;


public class GrievanceMapper {

    /**
     * Map create request to Grievance entity.
     */
    public static Grievance toEntity(GrievanceCreateRequest req,
                                     String businessId,
                                     List<DocumentMeta> documentMetas) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> userDetailsMap = mapper.convertValue(req.getUserDetails(), Map.class);

        Grievance grievance = new Grievance();
        grievance.setGrievanceId(UUID.randomUUID().toString());
        grievance.setUserType(req.getUserType());
        grievance.setBusinessId(businessId);
        grievance.setUserDetails(userDetailsMap); // store dynamic userDetails
        grievance.setGrievanceType(req.getGrievanceType());
        grievance.setGrievanceDetail(req.getGrievanceDetail());
        grievance.setGrievanceDescription(req.getGrievanceDescription());
        grievance.setSupportingDocs(documentMetas);
        grievance.setStatus(GrievanceStatus.NEW);
        grievance.setCreatedAt(LocalDateTime.now());
        grievance.setUpdatedAt(LocalDateTime.now());

        return grievance;
    }

    /**
     * Map Grievance entity to GrievanceResponse (after creation).
     */
    public static GrievanceResponse toResponse(Grievance grievance) {
        return GrievanceResponse.builder()
                .grievanceId(grievance.getGrievanceId())
                .grievanceTemplateId(grievance.getGrievanceTemplateId())
                .grievanceJwtToken(grievance.getGrievanceJwtToken())
                .status(grievance.getStatus())
//                .createdAt(grievance.getCreatedAt())
//                .updatedAt(grievance.getUpdatedAt())
                .message("Your grievance has been registered successfully.")
                .build();
    }

    /**
     * Map Grievance entity to list response (for pagination/list API).
     */
    public static GrievanceListResponse toListResponse(Grievance g) {
        List<SupportingDoc> docs = new ArrayList<>();
        if (g.getSupportingDocs() != null) {
            g.getSupportingDocs().forEach(docMeta -> {
                docs.add(SupportingDoc.builder()
                        .docName(docMeta.getName())
                        .doc(docMeta.getContentType())
                        .build());
            });
        }

        return GrievanceListResponse.builder()
                .grievanceId(g.getGrievanceId())
                .grievanceTemplateId(g.getGrievanceTemplateId())
                .feedback(g.getFeedback())
                .userType(g.getUserType())
                .userDetails(g.getUserDetails()) // dynamic userDetails map
                .grievanceType(g.getGrievanceType())
                .grievanceDetail(g.getGrievanceDetail())
                .grievanceDescription(g.getGrievanceDescription())
                .resolutionRemarks(g.getResolutionRemarks())
                .history(g.getHistory())
                .supportingDocs(docs)
                .status(g.getStatus())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    /**
     * Apply updates from GrievanceUpdateRequest safely.
     */
    /**
     * Apply updates from GrievanceUpdateRequest safely.
     */
    public static void applyUpdates(Grievance grievance, GrievanceUpdateRequest updates) {
        if (updates == null) return;

        // 1️⃣ Update status if provided
        if (updates.getStatus() != null) {
            try {
                GrievanceUpdateStatus updateStatus = updates.getStatus();

                // Map GrievanceUpdateStatus → GrievanceStatus
                GrievanceStatus mappedStatus = null;
                switch (updateStatus) {
                    case INPROCESS -> mappedStatus = GrievanceStatus.INPROCESS;
                    case RESOLVED -> mappedStatus = GrievanceStatus.RESOLVED;
                    default -> throw new InvalidRequestException("Unsupported grievance update status: " + updateStatus);
                }

                grievance.setStatus(mappedStatus);
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("Invalid grievance status: " + updates.getStatus());
            }
        }

        // 2️ Add remark to timeline
        if (updates.getResolutionRemark() != null && !updates.getResolutionRemark().isBlank()) {
            StatusTimeline timeline = StatusTimeline.builder()
                    .status(grievance.getStatus())
                    .remark(updates.getResolutionRemark())
                    .timestamp(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            if (grievance.getResolutionRemarks() == null) {
                grievance.setResolutionRemarks(new ArrayList<>());
            }
            grievance.getResolutionRemarks().add(timeline);
        }

        // 3️ Update modification timestamp
        grievance.setUpdatedAt(LocalDateTime.now());
    }


    /**
     * Map Grievance entity to a detailed response for GET by ID API.
     */
    public static GrievanceDetailResponse getByIdResponse(Grievance g) {
        List<SupportingDoc> docs = new ArrayList<>();
        if (g.getSupportingDocs() != null) {
            g.getSupportingDocs().forEach(docMeta -> {
                docs.add(SupportingDoc.builder()
                        .docName(docMeta.getName())
                        .doc(docMeta.getContentType())
                        .build());
            });
        }

        return GrievanceDetailResponse.builder()
                .grievanceId(g.getGrievanceId())
                .grievanceTemplateId(g.getGrievanceTemplateId())
                .grievanceJwtToken(g.getGrievanceJwtToken())
                .userType(g.getUserType())
                .userDetails(g.getUserDetails())
                .grievanceType(g.getGrievanceType())
                .grievanceDetail(g.getGrievanceDetail())
                .grievanceDescription(g.getGrievanceDescription())
                .supportingDocs(docs)
                .resolutionRemarks(g.getResolutionRemarks())
                .history(g.getHistory())
                .feedback(g.getFeedback())
                .status(g.getStatus())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}
