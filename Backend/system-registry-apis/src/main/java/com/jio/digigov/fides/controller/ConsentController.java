package com.jio.digigov.fides.controller;

import com.jio.digigov.fides.constant.HeaderConstants;
import com.jio.digigov.fides.dto.request.WithdrawlRequest;
import com.jio.digigov.fides.entity.ConsentWithdrawalJob;
import com.jio.digigov.fides.service.impl.ConsentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentServiceImpl consentService;

    @PostMapping("/{consentId}/withdraw")
    public ResponseEntity<ConsentWithdrawalJob> withdrawConsent(
            @PathVariable String consentId,
            @Valid @RequestBody WithdrawlRequest request,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId) {

        return new ResponseEntity<ConsentWithdrawalJob>(consentService.withdrawConsent(consentId, tenantId, businessId, request), HttpStatus.ACCEPTED);
    }
}