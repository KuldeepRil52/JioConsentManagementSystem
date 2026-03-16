package com.jio.digigov.auditmodule.config;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class FilterConfig implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            HttpServletRequest httpReq = (HttpServletRequest) request;

            // Fetch tenant and business IDs from headers (or any source)
            String tenantId = httpReq.getHeader("X-Tenant-ID");
            String businessId = httpReq.getHeader("X-Business-ID");

            // Use default if not provided
            if (tenantId == null || tenantId.isBlank()) tenantId = "default_tenant";
            if (businessId == null || businessId.isBlank()) businessId = "default_business";

            // Put in MDC so Logback can pick them
            MDC.put("tenantId", tenantId);
            MDC.put("businessId", businessId);

            chain.doFilter(request, response);

        } finally {
            // Always clear MDC to prevent data leak between threads
            MDC.clear();
        }
    }
}
