package com.jio.consent.service;

import com.jio.consent.constant.Constants;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.Request.CreateConsentMetaRequest;
import com.jio.consent.dto.Response.ConsentMetaResponse;
import com.jio.consent.entity.ConsentMeta;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.repository.ConsentMetaRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.UUID;

@Service
@Slf4j
public class ConsentMetaService {

    ConsentMetaRepository consentMetaRepository;

    @Autowired
    public ConsentMetaService(ConsentMetaRepository consentMetaRepository) {
        this.consentMetaRepository = consentMetaRepository;
    }

    public ConsentMetaResponse createConsentMeta(CreateConsentMetaRequest request) throws ConsentException {
        // Validate relation is required when isParentalConsent is true
        if (Boolean.TRUE.equals(request.getIsParentalConsent()) && ObjectUtils.isEmpty(request.getRelation())) {
            throw new ConsentException(ErrorCodes.JCMP1053);
        }

        String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
        String randomUUID = UUID.randomUUID().toString();
        String consentMetaId = tenantId + ":" + randomUUID;

        ConsentMeta.ConsentMetaBuilder consentMetaBuilder = ConsentMeta.builder()
                .consentMetaId(consentMetaId)
                .languagePreference(request.getLanguagePreference())
                .preferencesStatus(request.getPreferencesStatus())
                .isParentalConsent(request.getIsParentalConsent())
                .consentHandleId(request.getConsentHandleId())
                .secId(request.getSecId())
                .additionalInfo(request.getAdditionalInfo());

        // Add relation only if isParentalConsent is true
        if (Boolean.TRUE.equals(request.getIsParentalConsent())) {
            consentMetaBuilder.relation(request.getRelation());
        }

        ConsentMeta consentMeta = consentMetaBuilder.build();
        ConsentMeta savedConsentMeta = this.consentMetaRepository.save(consentMeta);

        ConsentMetaResponse.ConsentMetaResponseBuilder responseBuilder = ConsentMetaResponse.builder()
                .consentMetaId(savedConsentMeta.getConsentMetaId())
                .languagePreference(savedConsentMeta.getLanguagePreference())
                .preferencesStatus(savedConsentMeta.getPreferencesStatus())
                .isParentalConsent(savedConsentMeta.getIsParentalConsent())
                .consentHandleId(savedConsentMeta.getConsentHandleId())
                .secId(savedConsentMeta.getSecId())
                .additionalInfo(savedConsentMeta.getAdditionalInfo())
                .message("Consent Meta created successfully!");

        // Include relation in response only if isParentalConsent is true
        if (Boolean.TRUE.equals(savedConsentMeta.getIsParentalConsent())) {
            responseBuilder.relation(savedConsentMeta.getRelation());
        }

        return responseBuilder.build();
    }

    public ConsentMetaResponse getConsentMetaById(String consentMetaId) throws ConsentException {
        ConsentMeta consentMeta = this.consentMetaRepository.getByConsentMetaId(consentMetaId);
        
        if (ObjectUtils.isEmpty(consentMeta)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        ConsentMetaResponse.ConsentMetaResponseBuilder responseBuilder = ConsentMetaResponse.builder()
                .consentMetaId(consentMeta.getConsentMetaId())
                .languagePreference(consentMeta.getLanguagePreference())
                .preferencesStatus(consentMeta.getPreferencesStatus())
                .isParentalConsent(consentMeta.getIsParentalConsent())
                .consentHandleId(consentMeta.getConsentHandleId())
                .secId(consentMeta.getSecId())
                .additionalInfo(consentMeta.getAdditionalInfo());

        // Include relation in response only if isParentalConsent is true
        if (Boolean.TRUE.equals(consentMeta.getIsParentalConsent())) {
            responseBuilder.relation(consentMeta.getRelation());
        }

        return responseBuilder.build();
    }
}

