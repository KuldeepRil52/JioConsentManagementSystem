package com.jio.partnerportal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
public class CachingRequestResponseFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Wrap request/response so body can be read multiple times
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        // Extract tenantId
        String tenantId = request.getHeader("tenant-id");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        try {
            // Put tenant in MDC for logging
            MDC.put("tenantId", tenantId);

            // Proceed with filter chain
            filterChain.doFilter(requestWrapper, responseWrapper);


        } finally {
            responseWrapper.copyBodyToResponse(); // copy back to client
            MDC.clear(); // avoid leaks
        }
    }

}
