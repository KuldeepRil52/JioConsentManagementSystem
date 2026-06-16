package com.jio.consent.dto.Response;

import com.jio.consent.dto.ConsentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsentStatusCountResponse {

    private long totalCount;
    private Map<ConsentStatus, Long> statusCounts;

}
