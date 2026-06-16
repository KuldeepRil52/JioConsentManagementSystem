package com.jio.consent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
public class CachingRequestResponseFilter extends OncePerRequestFilter {

    @Value("${logging.http.trace.enabled:false}")
    private boolean httpTraceEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extract tenantId
        String tenantId = request.getHeader("tenant-id");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        try {
            // Put tenant in MDC
            MDC.put("tenantId", tenantId);

            if (httpTraceEnabled) {
                // Wrap request/response to allow body logging
                ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

                filterChain.doFilter(requestWrapper, responseWrapper);

                // Copy response back
                responseWrapper.copyBodyToResponse();

            } else {
                // Put tenant MDC context
                filterChain.doFilter(request, response);
            }

        } finally {
            MDC.clear();
        }
    }

}