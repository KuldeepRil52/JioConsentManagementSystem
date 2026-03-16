package com.jio.schedular.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jio.schedular.dto.Resource;
import com.jio.schedular.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "schedular_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedularStats {

    @Id
    private String id;

    private String runId;                 // unique per run
    private JobName jobName;
    private List<Resource> resources;
    private Group group;                 // job group / category
    private Action action;

    private SummaryType summaryType;           // TENANT_SUMMARY
    private int totalAffected;
    private int errorCount;
    private String lastError;
    private SchedularStatus status;      // SUCCESS, PARTIAL_SUCCESS, FAILED
    private Map<String, Object> details; // flexible JSON structure

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    private long durationMillis;
}