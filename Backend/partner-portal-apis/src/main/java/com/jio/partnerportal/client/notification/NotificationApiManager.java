package com.jio.partnerportal.client.notification;

import com.jio.partnerportal.util.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationApiManager extends RestApiManager {

    @Value("${notification.service.base.url}")
    private String notificationServiceBaseUrl;

    @Value("${notification.service.endpoints.onboarding-setup}")
    private String onboardingSetupEndpoint;

    @Value("${notification.service.endpoints.trigger-event}")
    private String triggerEventEndpoint;

    @Value("${notification.service.endpoints.system-trigger-event}")
    private String systemTriggerEventEndpoint;

    @Value("${notification.service.endpoints.system-verify-otp}")
    private String systemVerifyOtpEndpoint;

    public <REQ, RES> ResponseEntity<RES> postOnboardingSetup(Map<String, String> headers, REQ requestBody, Class<RES> responseType) {
        return super.post(notificationServiceBaseUrl, onboardingSetupEndpoint, headers, requestBody, responseType);
    }

    public <REQ, RES> ResponseEntity<RES> postTriggerEvent(Map<String, String> headers, REQ requestBody, Class<RES> responseType) {
        return super.post(notificationServiceBaseUrl, triggerEventEndpoint, headers, requestBody, responseType);
    }

    public <REQ, RES> ResponseEntity<RES> postSystemTriggerEvent(Map<String, String> headers, REQ requestBody, Class<RES> responseType) {
        return super.post(notificationServiceBaseUrl, systemTriggerEventEndpoint, headers, requestBody, responseType);
    }

    public <REQ, RES> ResponseEntity<RES> postSystemVerifyOtp(Map<String, String> headers, REQ requestBody, Class<RES> responseType) {
        return super.post(notificationServiceBaseUrl, systemVerifyOtpEndpoint, headers, requestBody, responseType);
    }
}
