package com.jio.consent.interceptor;

import com.jio.consent.constant.Constants;
import com.jio.consent.utils.Validation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;


@Component
@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    private final Validation validation;

    @Autowired
    public LogInterceptor(Validation validation) {
        this.validation = validation;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Generate UUID for request tracking
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        // Get full URL including query parameters
        String fullUrl = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        String url = queryString == null ? fullUrl : fullUrl + "?" + queryString;

        // Helper method to safely get header values with fallback
        String txnId = request.getHeader("txn");
        String version = request.getHeader("version");
        String tenantId = request.getHeader(Constants.TENANT_ID_HEADER);
        String httpMethod = request.getMethod() != null ? request.getMethod() : "-";
        String apiPath = request.getServletPath() != null ? request.getServletPath() : "-";
        String sourceIp = request.getRemoteAddr() != null ? request.getRemoteAddr() : "-";

        // Set values in ThreadContext (for Log4j2 compatibility) - never null
        ThreadContext.put("START", Long.toString(startTime));
        ThreadContext.put("TXN_ID", txnId != null ? txnId : "-");
        ThreadContext.put("API_NAME", apiPath);
        ThreadContext.put("VERSION", version != null ? version : "-");
        ThreadContext.put("HTTP_METHOD", httpMethod);
        ThreadContext.put("SOURCE_IP", sourceIp);
        ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId != null ? tenantId : "-");
        ThreadContext.put("UUID", uuid);
        ThreadContext.put("URL", url);
        
        // Set values in MDC for Logback pattern - using ThreadContext key names
        MDC.put("SOURCE_IP", sourceIp);
        MDC.put(Constants.TENANT_ID_HEADER, tenantId != null ? tenantId : "-");
        MDC.put("UUID", uuid);
        MDC.put("HTTP_METHOD", httpMethod);
        MDC.put("URL", url);
        MDC.put("tenantId", tenantId != null ? tenantId : "-");

        this.validation.validateTenantIdHeader();
        this.validation.validateTxnHeader(request.getHeader("txn"));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {

        String startTimeStr = ThreadContext.get("START");
        long start = startTimeStr != null ? Long.parseLong(startTimeStr) : System.currentTimeMillis();
        long end = System.currentTimeMillis();
        long tat = end - start;
        int statusCode = response.getStatus();
        if (ex == null && statusCode >= 200 && statusCode < 300) {
            log.info("Status: SUCCESS, HTTP_RESPONSE_CODE: {}, TAT: {}", response.getStatus(), tat);
        } else {
            String errorMessage = ex != null ? ex.getMessage() : "HTTP request failed";
            log.error("Status: FAILURE, HTTP_RESPONSE_CODE: {}, TAT: {} ms, Error: {}", 
                    response.getStatus(), tat, errorMessage, ex);
        }

        // Clear MDC and ThreadContext
        MDC.clear();
        ThreadContext.clearMap();
    }
}
