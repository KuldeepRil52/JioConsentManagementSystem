package com.jio.auth.controller;

import com.jio.auth.service.Wso2TokenManagerService;
import com.jio.auth.service.Wso2TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/token")
public class Wso2TokenController {

    @Autowired
    private Wso2TokenManagerService tokenManagerService;

    @Autowired
    private Wso2TokenService tokenService;

    @GetMapping
    public String getAccessToken(
            @RequestHeader("tenantId") String tenantId,
            @RequestHeader("businessId") String businessId) {

        System.out.println("=== Fetching new token for tenant: " + tenantId + ", business: " + businessId + " ===");

        String token = tokenManagerService.getValidAccessToken(tenantId, businessId);
        System.out.println("Access token fetched successfully: " + token);

        return token;
    }
}
