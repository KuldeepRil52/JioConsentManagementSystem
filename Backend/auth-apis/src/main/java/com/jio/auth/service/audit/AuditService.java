package com.jio.auth.service.audit;

import com.jio.auth.model.FailedAudit;
import com.jio.auth.dto.AuditDto;
import com.jio.auth.dto.AuditStructuredDto;
import com.jio.auth.repository.FailedAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final FailedAuditRepository failedRepo;
    private final RestTemplate restTemplate;

    @Value("${audit.url}")
    private String auditUrl;

    public void sendAudit(AuditDto dto) {
        log.info("Sending audit request");

        if (dto.getTenantId() == null || dto.getTenantId().isBlank()) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("x-tenant-id", dto.getTenantId());
            headers.set("x-business-id", dto.getBusinessId());
            headers.set("x-transaction-id", UUID.randomUUID().toString());

            AuditStructuredDto auditDto = toStructured(dto);

            HttpEntity<AuditStructuredDto> entity = new HttpEntity<>(auditDto, headers);

            ResponseEntity<String> response =
                    createAuditTemplate().postForEntity(auditUrl, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.info("Failed Audit {}", response.getStatusCode().value());
                saveFailed(dto);
            }

        } catch (Exception e) {
            log.error("failed to send to audit {}", e.getMessage());
            saveFailed(dto);
        }
    }

    private RestTemplate createAuditTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 sec
        factory.setReadTimeout(5000);    // 5 sec
        return new RestTemplate(factory);
    }


    public void sendAudit(AuditDto dto, String failit) {
        log.info("Sending audit request from {}", failit);
        if (dto.getTenantId() == null || dto.getTenantId().isBlank()) {
            return;
        }
        log.info("Audit Saved to DB for scheduler to run it");
        saveFailed(dto);

    }

    private void saveFailed(AuditDto dto) {
        FailedAudit fa = new FailedAudit();

        fa.setId(UUID.randomUUID().toString());
        fa.setTxnId(dto.getTxnId());
        fa.setTenantId(dto.getTenantId());
        fa.setBusinessId(dto.getBusinessId());
        fa.setTransactionId(dto.getTransactionId());
        fa.setIp(dto.getIp());
        fa.setRequestUri(dto.getRequestUri());
        fa.setMethod(dto.getMethod());
        fa.setRequestPayload(dto.getRequestPayload());
        fa.setResponsePayload(dto.getResponsePayload());
        fa.setActor(dto.getActor());
        fa.setCreatedAt(new Date());

        failedRepo.save(fa);
    }
    public AuditStructuredDto toStructured(AuditDto dto) {

        AuditStructuredDto out = new AuditStructuredDto();

        // top-level
        out.setTenantId(dto.getTenantId());
        out.setBusinessId(dto.getBusinessId());
        out.setGroup("AUTH-HANDLER");  // fixed
        out.setComponent(null);        // always null

        String action = (dto.getResponsePayload() != null) ? "SUCCESS" : "FAILED";
        out.setAction(action);

        // Actor
        AuditStructuredDto.Actor atr = new AuditStructuredDto.Actor();
        atr.setId(dto.getActor());
        atr.setRole("sub");

        // context block
        AuditStructuredDto.Context ctx = new AuditStructuredDto.Context();
        ctx.setTxnId(dto.getTxnId());
        ctx.setIpAddress(dto.getIp());
        out.setContext(ctx);

        // resource block
        AuditStructuredDto.Resource res = new AuditStructuredDto.Resource();
        res.setId(dto.getTransactionId());
        res.setType(dto.getMethod() + ": " + dto.getRequestUri());
        out.setResource(res);

        return out;
    }


}
