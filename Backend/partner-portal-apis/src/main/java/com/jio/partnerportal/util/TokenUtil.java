package com.jio.partnerportal.util;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import java.util.HashMap;
import java.util.Map;

public class TokenUtil {

    private static String accessToken;
    private static long expiryTime;
    @Autowired
    private RestUtility restUtility;

    public static synchronized String getAccessToken(RestUtility restUtility, Environment env) throws Exception {
        if (accessToken == null || System.currentTimeMillis() >= expiryTime) {
            fetchNewToken(restUtility, env);
        }
        return accessToken;
    }

    private static void fetchNewToken(RestUtility restUtility, Environment env) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "client_credentials");

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", env.getProperty("NOTIFICATION_TOKEN_AUTH"));

        ResponseEntity<String> response = restUtility.callApiBypassSSL(
                env.getProperty("TOKEN_URL"),
                HttpMethod.POST,
                body,
                headers
        );

        JSONObject json = new JSONObject(response.getBody());
        accessToken = json.getString("access_token");

        int expiresIn = json.getInt("expires_in"); // seconds
        expiryTime = System.currentTimeMillis() + (expiresIn - 30) * 1000L; // buffer of 30s
    }
}
