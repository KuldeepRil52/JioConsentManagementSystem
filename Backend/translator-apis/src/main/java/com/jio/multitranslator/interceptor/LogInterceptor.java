package com.jio.multitranslator.interceptor;

import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.utils.Validation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    private final Validation validation;

    @Autowired
    public LogInterceptor(Validation validation){
        this.validation = validation;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String txn = request.getHeader("txn");
        String apiName = request.getServletPath();
        String method = request.getMethod();
        String tenantId = request.getHeader(Constants.TENANT_ID_HEADER);
        String businessId = request.getHeader(Constants.BUSINESS_ID_HEADER);
        
        ThreadContext.put("START", Long.toString(System.currentTimeMillis()));
        ThreadContext.put("TXN_ID", txn);
        ThreadContext.put("API_NAME", apiName);
        ThreadContext.put("VERSION", request.getHeader("version"));
        ThreadContext.put("HTTP_METHOD", method);
        ThreadContext.put("SOURCE_IP", request.getRemoteAddr());
        ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
        ThreadContext.put(Constants.BUSINESS_ID_HEADER, businessId);

        log.info("Request received - Method: {}, API: {}, TXN: {}, TenantId: {}, BusinessId: {}, IP: {}", 
                method, apiName, txn, tenantId, businessId, request.getRemoteAddr());

        try {
            this.validation.validateTenantHeader();
            this.validation.validateTxnHeader(txn);
            this.validation.validateBusinessIdHeader();
            log.debug("Request validation passed - TXN: {}, API: {}", txn, apiName);
        } catch (Exception e) {
            log.warn("Request validation failed - TXN: {}, API: {}, Error: {}", txn, apiName, e.getMessage());
            throw e;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        String startTime = ThreadContext.get("START");
        String apiName = ThreadContext.get("API_NAME");
        String txnId = ThreadContext.get("TXN_ID");
        int statusCode = response != null ? response.getStatus() : 0;
        
        if (startTime != null) {
            try {
                long start = Long.parseLong(startTime);
                long end = System.currentTimeMillis();
                long tat = end - start;
                
                if (ex != null) {
                    log.error("Request completed with exception - API: {}, TXN: {}, Status: {}, TAT: {}ms, Error: {}", 
                            apiName, txnId, statusCode, tat, ex.getMessage(), ex);
                } else if (statusCode >= 400) {
                    log.warn("Request completed with error status - API: {}, TXN: {}, Status: {}, TAT: {}ms", 
                            apiName, txnId, statusCode, tat);
                } else {
                    log.info("Request completed successfully - API: {}, TXN: {}, Status: {}, TAT: {}ms", 
                            apiName, txnId, statusCode, tat);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid START time in ThreadContext - API: {}, TXN: {}, StartTime: {}", 
                        apiName, txnId, startTime);
            }
        } else {
            log.warn("Missing START time in ThreadContext - API: {}, TXN: {}", apiName, txnId);
        }
        
        ThreadContext.clearMap();
    }
}
