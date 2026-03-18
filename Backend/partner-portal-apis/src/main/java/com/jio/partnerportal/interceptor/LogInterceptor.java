package com.jio.partnerportal.interceptor;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.TransactionLog;
import com.jio.partnerportal.repository.TransactionLogRepository;
import com.jio.partnerportal.util.Validation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@Slf4j
public class LogInterceptor implements HandlerInterceptor {
    private final Validation validation;
    private final TransactionLogRepository transactionLogRepository;

    @Autowired
    public LogInterceptor(Validation validation,
                          TransactionLogRepository transactionLogRepository) {
        this.validation = validation;
        this.transactionLogRepository = transactionLogRepository;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        //with tenant-id and no xsessiontoken
        if (Constants.INCLUDED_PATHS.stream().anyMatch(path -> request.getRequestURI().startsWith(path))){
            ThreadContext.put(Constants.TENANT_ID_HEADER, request.getHeader(Constants.TENANT_ID_HEADER));
            validation.validateTenantIdHeader();
        }
        //with xsessiontoken , tenantid from xsessiontoken
        else if (Constants.EXCLUDED_PATHS.stream().noneMatch(path -> request.getRequestURI().startsWith(path))) {
            validation.validateAuth(request.getHeader("x-session-token"));
        }

        ThreadContext.put("START", Long.toString(System.currentTimeMillis()));
        ThreadContext.put("TXN_ID", request.getHeader("txn"));
        ThreadContext.put("API_NAME", request.getServletPath());
        ThreadContext.put("VERSION", request.getHeader("version"));
        ThreadContext.put("HTTP_METHOD", request.getMethod());
        ThreadContext.put("SOURCE_IP", request.getRemoteAddr());

        validation.validateTxnHeader(request.getHeader("txn"));
        return true;


    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {

        long start = Long.parseLong(ThreadContext.get("START"));
        long end = System.currentTimeMillis();
        long tat = end - start;

        // Extract request/response bodies (wrappers are guaranteed by filter)
        String requestBody = "N/A";
        String responseBody = "N/A";

        if (request instanceof ContentCachingRequestWrapper reqWrapper) {
            byte[] buf = reqWrapper.getContentAsByteArray();
            if (buf.length > 0) {
                String encoding = reqWrapper.getCharacterEncoding() != null
                        ? reqWrapper.getCharacterEncoding()
                        : StandardCharsets.UTF_8.name();
                requestBody = new String(buf, encoding);
            }
        }

        if (response instanceof ContentCachingResponseWrapper resWrapper) {
            byte[] buf = resWrapper.getContentAsByteArray();
            if (buf.length > 0) {
                String encoding = resWrapper.getCharacterEncoding() != null
                        ? resWrapper.getCharacterEncoding()
                        : StandardCharsets.UTF_8.name();
                responseBody = new String(buf, encoding);
            }
            resWrapper.copyBodyToResponse(); // important
        }

        int statusCode = response.getStatus();

        if (ex == null && statusCode >= 200 && statusCode < 300) {
            log.info("Status: SUCCESS, HTTP_RESPONSE_CODE: {}, TAT: {}", response.getStatus(), tat);
        } else {
            String errorMessage = ex != null ? ex.getMessage() : "HTTP request failed";
            log.error("Status: FAILURE, HTTP_RESPONSE_CODE: {}, TAT: {} ms, Error: {}",
                    response.getStatus(), tat, errorMessage, ex);
        }

        TransactionLog logEntry = TransactionLog.builder()
                .txnId(ThreadContext.get("TXN_ID"))
                .apiName(ThreadContext.get("API_NAME"))
                .httpStatus(response.getStatus())
                .version(ThreadContext.get("VERSION"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .errorResponse(ex != null ? ex.getMessage() : null)
                .request(requestBody)
                .response(responseBody)
                .responseTime(tat)
                .build();

        transactionLogRepository.save(logEntry);

        ThreadContext.clearMap();
    }
}
