package com.jio.partnerportal.client.wso2.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@lombok.Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardBusinessResponse {

    private boolean success;
    private String message;
    private String code;
    private OnboardBusinessResponse.Data data;
    private String timestamp;


    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Data {
        private String consumerKey;
        private String consumerSecret;
        private String businessUniqueId;
    }

}
