package com.jio.auth.service;

import com.jio.auth.config.TenantMongoTemplateFactory;
import com.jio.auth.constants.ErrorCode;
import com.jio.auth.exception.CustomException;
import com.jio.auth.model.Wso2AccessToken;
import com.jio.auth.repository.Wso2AccessTokenRepository;
import com.jio.auth.repository.Wso2BusinessApplicationRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class Wso2TokenManagerService {

    @Autowired
    private Wso2BusinessApplicationRepository businessAppRepository;

    @Autowired
    private Wso2AccessTokenRepository accessTokenRepository;

    @Autowired
    private Wso2TokenService wso2TokenService;

    @Autowired
    private TenantMongoTemplateFactory tenantMongoTemplateFactory;

    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    private static final String FIELD_CONSUMER_KEY = "consumerKey";
    private static final String FIELD_CONSUMER_SECRET = "consumerSecret";
    private static final String FIELD_ACCESS_TOKEN = "access_token";
    private static final String FIELD_EXPIRES_IN = "expires_in";
    private static final String COLLECTION_TOKENS = "wso2_access_tokens";
    private static final String BUSINESS_ID = "businessId";

    private long EXPIRY_BUFFER_SEC = 10;

    public void generateAndStoreToken(String tenantId, String businessId) {
        // Fetch credentials from tenant DB

        Document creds = businessAppRepository.findByBusinessId(tenantId, businessId);

        if (creds == null) {
            System.out.println("No credentials found for businessId: " + businessId);
            throw new RuntimeException("No credentials found for businessId: " + businessId);
        }
        String consumerKey = creds.getString(FIELD_CONSUMER_KEY);
        String consumerSecret = creds.getString(FIELD_CONSUMER_SECRET);

        Map<String, Object> tokenResponse = wso2TokenService.getAccessToken(consumerKey, consumerSecret);
        if (tokenResponse == null || !tokenResponse.containsKey(FIELD_ACCESS_TOKEN)) {
            log.info("WSO2 token generation failed for businessId: " + businessId);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "WSO2 token generation failed for businessId: " + businessId);
        }

        log.info("tokenResponse: " + tokenResponse);
        String accessToken = tokenResponse.get(FIELD_ACCESS_TOKEN).toString();
        long expiresIn = Long.parseLong(tokenResponse.get(FIELD_EXPIRES_IN).toString());
        long expiresAt = Instant.now().getEpochSecond() + expiresIn;

        Wso2AccessToken token = new Wso2AccessToken();
        token.setBusinessId(businessId);
        token.setAccessToken(accessToken);
        token.setExpiresAt(expiresAt);

        accessTokenRepository.deleteByBusinessId(tenantId, businessId);
        accessTokenRepository.save(tenantId, token);
        log.info("token saved in the DB" );
    }

    public String getValidAccessToken(String tenantId, String businessId) {
        Wso2AccessToken token = accessTokenRepository.findByBusinessId(tenantId, businessId);
        long currentTimeSec;
        String lockKey = tenantId + ":" + businessId;
        Object lock = lockMap.computeIfAbsent(lockKey, k -> new Object());

        synchronized (lock) {
            token = accessTokenRepository.findByBusinessId(tenantId, businessId);
            currentTimeSec = Instant.now().getEpochSecond();

            if (token == null || token.getExpiresAt() < currentTimeSec + EXPIRY_BUFFER_SEC) {
                generateAndStoreToken(tenantId, businessId);
                token = accessTokenRepository.findByBusinessId(tenantId, businessId);
                log.info("Token regenerated and fetched after synchronization");
            }
        }

        if (token == null) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR,
                    "Failed to fetch or refresh token from db for businessId: " + businessId);
        }
        return token.getAccessToken();
    }
}
