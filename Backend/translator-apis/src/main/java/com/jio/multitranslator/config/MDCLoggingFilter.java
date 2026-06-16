package com.jio.multitranslator.config;

import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.repository.TenantRegistryRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MDCLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MDCLoggingFilter.class);

    TenantRegistryRepository tenantRegistryRepository;

    public MDCLoggingFilter(TenantRegistryRepository tenantRegistryRepository){
        this.tenantRegistryRepository = tenantRegistryRepository;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // --- Extract values from headers or params ---
            String tenantId = request.getHeader(Constants.TENANT_ID_HEADER);
            String businessId = request.getHeader(Constants.BUSINESS_ID_HEADER);
            String sourceIp = request.getRemoteAddr();

            // --- Validate tenantId ---
            if (tenantId == null || tenantId.isBlank() || !this.tenantRegistryRepository.existsByTenantId(tenantId)) {
                log.warn("⚠️ Invalid or missing tenantId: '{}', falling back to 'default'", tenantId);
                tenantId = "default";
            }

            if (businessId == null || businessId.isBlank()) {
                businessId = "NA"; // default for missing businessId
            }

            // --- Put context values in MDC ---
            MDC.put(Constants.TENANT_ID_HEADER, tenantId);
            MDC.put(Constants.BUSINESS_ID_HEADER, businessId);
            MDC.put("SOURCE_IP", sourceIp);

            // Continue request
            filterChain.doFilter(request, response);

        } finally {
            // --- Clean up MDC context after each request ---
            MDC.clear();
        }
    }

}