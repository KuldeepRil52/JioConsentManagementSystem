package com.jio.digigov.auditmodule.filter;

import com.jio.digigov.auditmodule.util.TenantContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import java.io.IOException;

/**
 * Servlet Filter to attach tenant and business context info into MDC (for logging).
 */
@Slf4j
public class TenantLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            if (request instanceof HttpServletRequest httpReq) {
                String tenantId = httpReq.getHeader("X-Tenant-ID");
                String businessId = httpReq.getHeader("X-Business-ID");

                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContextHolder.setTenantId(tenantId);
                    MDC.put("tenantId", tenantId);
                }
                if (businessId != null && !businessId.isBlank()) {
                    TenantContextHolder.setBusinessId(businessId);
                    MDC.put("businessId", businessId);
                }

                log.debug("TenantLoggingFilter applied: tenantId={}, businessId={}",
                        tenantId, businessId);
            }

            chain.doFilter(request, response);
        } finally {
            // Clean up context after request completes
            MDC.remove("tenantId");
            MDC.remove("businessId");
            TenantContextHolder.clear();
        }
    }
}