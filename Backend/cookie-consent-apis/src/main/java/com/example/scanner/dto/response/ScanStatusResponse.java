package com.example.scanner.dto.response;

import com.example.scanner.entity.CookieEntity;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ScanStatusResponse {
    private String transactionId;
    private String status;
    private String url;
    private Integer totalCookies;
    private List<SubdomainCookieGroup> subdomains;
    private ScanSummary summary;

    public ScanStatusResponse(String transactionId, String status, String url,
                              List<SubdomainCookieGroup> subdomains, ScanSummary summary) {
        this.transactionId = transactionId;
        this.status = status;
        this.url = url;
        this.subdomains = subdomains;
        this.summary = summary;
        this.totalCookies = subdomains != null ? subdomains.stream()
                .mapToInt(SubdomainCookieGroup::getCookieCount)
                .sum() : 0;
    }

    @Data
    public static class SubdomainCookieGroup {
        private String subdomainName;
        private String subdomainUrl;
        private Integer cookieCount;
        private List<CookieEntity> cookies;

        public SubdomainCookieGroup(String subdomainName, String subdomainUrl, List<CookieEntity> cookies) {
            this.subdomainName = subdomainName;
            this.subdomainUrl = subdomainUrl;
            this.cookies = cookies;
            this.cookieCount = cookies != null ? cookies.size() : 0;
        }
    }

    @Data
    public static class ScanSummary {
        private Map<String, Integer> bySource;
        private Map<String, Integer> byCategory;

        public ScanSummary(Map<String, Integer> bySource, Map<String, Integer> byCategory) {
            this.bySource = bySource;
            this.byCategory = byCategory;
        }
    }
}