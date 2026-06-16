package com.jio.digigov.fides.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class DatasetListResponse {
    private int totalRecords;
    private List<DatasetResponse> datasets;
}


