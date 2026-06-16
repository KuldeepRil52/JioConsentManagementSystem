package com.jio.partnerportal.client.wso2.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterTenantResponse {

    private boolean success;
    private String message;
    private String code;
    private Data data;
    private String timestamp;


    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Data {
        private String tenantUniqueId;
        private String message;
    }


}
