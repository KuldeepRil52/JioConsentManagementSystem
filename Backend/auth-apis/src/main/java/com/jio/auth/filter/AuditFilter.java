package com.jio.auth.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.auth.dto.AuditDto;
import com.jio.auth.service.audit.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.UUID;

@Component
public class AuditFilter extends OncePerRequestFilter {

    @Autowired
    private static AuditService auditService;

    private static final Logger audit = LoggerFactory.getLogger("AUDIT_LOGGER");

    public AuditFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        BufferingResponseWrapper wrapped = new BufferingResponseWrapper(response);

        long start = System.currentTimeMillis();
        filterChain.doFilter(request, wrapped);
        long took = System.currentTimeMillis() - start;

        String responseBody = wrapped.getBody();
        String tenantId = request.getHeader("tenant-id");
        String businessId = request.getHeader("business-id");

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = wrapped.getHeader("tenant-id");
        }

        if (businessId == null || businessId.isBlank()) {
            businessId = wrapped.getHeader("business-id");
        }
        audit.info(
                "UserIP={} | BusinessId={} | TenantId={} | Time={}ms | Txn={} | URI={} | Method={} | Status={} | Response={}",
                request.getRemoteAddr(),
                businessId,
                tenantId,
                took,
                request.getHeader("txn"),
                request.getRequestURI(),
                request.getMethod(),
                wrapped.getStatus(),
                responseBody
        );
        ObjectMapper mapper = new ObjectMapper();
        String sub = null;

        try {
            JsonNode respJson = mapper.readTree(responseBody);
            JsonNode subNode = respJson.path("sub");

            if (!subNode.isMissingNode()) {
                sub = subNode.asText();
            }
        } catch (Exception ignored) {}

        audit.info("SUB FOUND = {}", sub);
        //sendToAudit(request, responseBody, tenantId, businessId, sub);
        wrapped.copyBodyToResponse();
    }
    private void sendToAudit(HttpServletRequest request, String responseBody, String tenantId, String businessId, String sub) {
        AuditDto dto = new AuditDto();
        dto.setTxnId(request.getHeader("txn"));
        dto.setTenantId(tenantId);
        dto.setBusinessId(businessId);
        dto.setTransactionId(UUID.randomUUID().toString());
        dto.setIp(request.getRemoteAddr());
        dto.setRequestUri(request.getRequestURI());
        dto.setMethod(request.getMethod());
        dto.setRequestPayload(null);
        dto.setResponsePayload(responseBody);
        dto.setActor(sub);
        auditService.sendAudit(dto, "AuditFilter");
    }

    private static class BufferingResponseWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private ServletOutputStream out;
        private PrintWriter writer;

        public BufferingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            if (out == null) {
                out = new ServletOutputStream() {
                    @Override
                    public void write(int b) {
                        buffer.write(b);
                    }
                    @Override
                    public boolean isReady() {
                        return true;
                    }
                    @Override
                    public void setWriteListener(WriteListener listener) {}
                };
            }
            return out;
        }

        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                try {
                    writer = new PrintWriter(new OutputStreamWriter(buffer, getCharacterEncoding()), true);
                } catch (Exception e) {
                    writer = new PrintWriter(buffer, true);
                }
            }
            return writer;
        }

        public String getBody() {
            try {
                if (writer != null) writer.flush();
                return buffer.toString(getCharacterEncoding());
            } catch (Exception e) {
                return buffer.toString();
            }
        }

        public void copyBodyToResponse() throws IOException {
            if (writer != null) writer.flush();
            byte[] bytes = buffer.toByteArray();
            ServletOutputStream out = super.getOutputStream();
            out.write(bytes);
            out.flush();
        }



    }
}
