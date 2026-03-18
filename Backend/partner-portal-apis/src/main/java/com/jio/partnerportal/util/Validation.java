package com.jio.partnerportal.util;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.DataCatagory;
import com.jio.partnerportal.dto.DocumentMeta;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.dto.LANGUAGE;
import com.jio.partnerportal.dto.LanguageTypographySettings;
import com.jio.partnerportal.dto.NotificationDetails;
import com.jio.partnerportal.dto.request.*;
import com.jio.partnerportal.dto.request.DigiLockerCredentialRequest;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.TenantRepository;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import static com.jio.partnerportal.constant.Constants.PAN;

@Slf4j
@Component
public class Validation {

    TenantRepository tenantRepository;
    AuthUtility authUtility;

    public Validation(TenantRepository tenantRepository,
                      AuthUtility authUtility) {
        this.tenantRepository = tenantRepository;
        this.authUtility = authUtility;
    }
    
    private static final Set<String> ALLOWED_FONT_TYPES = Set.of(
            "font/ttf",
            "font/otf",
            "font/woff",
            "font/woff2",
            "application/x-font-ttf",
            "application/x-font-otf",
            "application/font-woff");

    @Value("${x-session-token.enable:false}")
    private boolean authEnabled;

    public void validateOnboardTenantRequest(OnboardTenantRequest request) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(request.getPan())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, PAN);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getCompanyName())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.COMPANY_NAME);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getSpoc())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.SPOC);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getSpoc().getName())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.NAME);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getIdentityType())) {
            String errorCode = ErrorCodes.JCMP1002;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.IDENTITY_TYPE);
            errs.add(error);
        } else {
            if (request.getIdentityType().equals(IdentityType.EMAIL)) {
                if (ObjectUtils.isEmpty(request.getSpoc()) || ObjectUtils.isEmpty(request.getSpoc().getEmail())) {
                    String errorCode = ErrorCodes.JCMP1002;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.EMAIL);
                    errs.add(error);
                } else {
                    Pattern txnPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
                    Matcher match = txnPattern.matcher(request.getSpoc().getEmail());
                    if (!match.find()) {
                        String errorCode = ErrorCodes.JCMP1002;
                        Map<String, Object> error = getErrorDetails(errorCode, Constants.EMAIL);
                        errs.add(error);
                    }
                }
            }
            if (request.getIdentityType().equals(IdentityType.MOBILE)) {
                if ((ObjectUtils.isEmpty(request.getSpoc()) || ObjectUtils.isEmpty(request.getSpoc().getMobile()))) {
                    String errorCode = ErrorCodes.JCMP1002;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.MOBILE);
                    errs.add(error);
                } else {
                    Pattern txnPattern = Pattern.compile("^[6-9]\\d{9}$");
                    Matcher match = txnPattern.matcher(request.getSpoc().getMobile());
                    if (!match.find()) {
                        String errorCode = ErrorCodes.JCMP1002;
                        Map<String, Object> error = getErrorDetails(errorCode, Constants.MOBILE);
                        errs.add(error);
                    }
                }
            }
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    private static Map<String, Object> getErrorDetails(String errorCode, String parameter) {
        Map<String, Object> errMap = new HashMap<>();
        errMap.put(Constants.ERROR_CODE, errorCode);
        errMap.put(Constants.PARAMETER, parameter);
        return errMap;
    }

    public void validateTenantExistence(String pan) throws BodyValidationException {
        boolean isPresent = this.tenantRepository.findByPan(pan).isPresent();
        if (!isPresent) {
            return;
        }
        List<Map<String, Object>> errs = new ArrayList<>();
        String errorCode = ErrorCodes.JCMP1003;
        Map<String, Object> error = getErrorDetails(errorCode, Constants.PAN);
        errs.add(error);

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateTxnHeader(String txn) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(txn)) {
            String errorCode = ErrorCodes.JCMP2001;
            Map<String, Object> error = getHeaderErrorDetails(errorCode, Constants.TXN_ID);
            errs.add(error);
        } else {
            try {
                UUID.fromString(txn);
            } catch (IllegalArgumentException e) {
                String errorCode = ErrorCodes.JCMP2001;
                Map<String, Object> error = getHeaderErrorDetails(errorCode, Constants.TXN_ID);
                errs.add(error);
            }
        }
        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public Map<String, Object> getHeaderErrorDetails(String errorCode, String header) {
        Map<String, Object> errMap = new HashMap<>();
        errMap.put(Constants.ERROR_CODE, errorCode);
        errMap.put(Constants.HEADER, header);
        return errMap;
    }

    public void validateLegalEntityUpdateRequest(String legalEntityId, LegalEntityUpdateRequest request) throws PartnerPortalException, BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(request.getCompanyName())) {
            String errorCode = ErrorCodes.JCMP1002;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.COMPANY_NAME);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getLogoUrl())) {
            String errorCode = ErrorCodes.JCMP1003;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.LOGO_URL);
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }

        if (ObjectUtils.isEmpty(legalEntityId) || ObjectUtils.isEmpty(request.getLegalEntityId()) || !legalEntityId.equals(request.getLegalEntityId())) {
            throw new PartnerPortalException(ErrorCodes.JCMP1009);
        }
    }

    public void validateTenantIdHeader() throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(ThreadContext.get(Constants.TENANT_ID_HEADER)) || !this.tenantRepository.existsByTenantId(ThreadContext.get(Constants.TENANT_ID_HEADER))) {
            Map<String, Object> error = getHeaderErrorDetails(ErrorCodes.JCMP2001, Constants.TENANT_ID_HEADER);
            errs.add(error);
        }
        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }
    
    public void validateBusinessIdHeader(Map<String, String> headers) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (headers.get(Constants.BUSINESS_ID_HEADER)==null) {
            Map<String, Object> error = getHeaderErrorDetails(ErrorCodes.JCMP2001, Constants.BUSINESS_ID_HEADER);
            errs.add(error);
        }
        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateConfigHeader(Map<String, String> headers) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (!headers.containsKey(Constants.BUSINESS_ID_HEADER)) {
            Map<String, Object> error = getHeaderErrorDetails(ErrorCodes.JCMP2001, Constants.BUSINESS_ID_HEADER);
            errs.add(error);
        }
        if (!headers.containsKey(Constants.SCOPE_LEVEL_HEADER)) {
            Map<String, Object> error = getHeaderErrorDetails(ErrorCodes.JCMP2001, Constants.SCOPE_LEVEL_HEADER);
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateRopaHeader(Map<String, String> headers) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (!headers.containsKey(Constants.BUSINESS_ID_HEADER)) {
            Map<String, Object> error = getHeaderErrorDetails(ErrorCodes.JCMP2001, Constants.BUSINESS_ID_HEADER);
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validatePurposeRequest(PurposeRequest request, Map<String, String> headers) throws BodyValidationException {
        validateConfigHeader(headers);
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(request.getPurposeName())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.PURPOSE_NAME);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getPurposeCode())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.PURPOSE_CODE);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getPurposeDescription())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.PURPOSE_DESCRIPTION);
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateDataTypeRequest(DataTypeRequest request, Map<String, String> headers) throws BodyValidationException {
        validateConfigHeader(headers);
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(request.getDataTypeName())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_TYPE_NAME);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getDataItems())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_ITEMS);
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateDataProcessorRequest(DataProcessorRequest request, Map<String, String> headers) throws BodyValidationException {
        validateConfigHeader(headers);
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(request.getDataProcessorName())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_PROCESSOR_NAME);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getCallbackUrl())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.CALLBACK_URL);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getDetails())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.DETAILS);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getAttachment())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.ATTACHMENT);
            errs.add(error);
        }

        if (ObjectUtils.isEmpty(request.getVendorRiskDocument())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.VENDOR_RISK_DOCUMENT);
            errs.add(error);
        }

        validateDocumentMeta(request.getAttachmentMeta());
        validateDocumentMeta(request.getVendorRiskDocumentMeta());

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    private void validateDocumentMeta(DocumentMeta attachmentMeta) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(attachmentMeta)) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.ATTACHMENT_META);
            errs.add(error);
        } else {
            if (ObjectUtils.isEmpty(attachmentMeta.getName())) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.NAME);
                errs.add(error);
            }
            if (ObjectUtils.isEmpty(attachmentMeta.getContentType())) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.CONTENT_TYPE);
                errs.add(error);
            }
            if (ObjectUtils.isEmpty(attachmentMeta.getSize())) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.SIZE);
                errs.add(error);
            }
            if (ObjectUtils.isEmpty(attachmentMeta.getTag())) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.TAG);
                errs.add(error);
            } else {
                if (ObjectUtils.isEmpty(attachmentMeta.getTag().getDocumentTag())) {
                    String errorCode = ErrorCodes.JCMP1001;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.DOCUMENT_TAG);
                    errs.add(error);
                }
                if (ObjectUtils.isEmpty(attachmentMeta.getTag().getComponents())) {
                    String errorCode = ErrorCodes.JCMP1001;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.COMPONENTS);
                    errs.add(error);
                }
            }
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateProcessorActivityRequest(ProcessorActivityRequest request, Map<String, String> headers) throws BodyValidationException {
        validateConfigHeader(headers);
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(request.getActivityName())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.ACTIVITY_NAME);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getProcessorName())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.PROCESSOR_NAME);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getDataTypeList())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_TYPE_LIST);
            errs.add(error);
        } else {
            for (DataCatagory dataCatagory : request.getDataTypeList()) {
                if (ObjectUtils.isEmpty(dataCatagory.getDataTypeId())) {
                    String errorCode = ErrorCodes.JCMP1001;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_TYPE_ID);
                    errs.add(error);
                }
                if (ObjectUtils.isEmpty(dataCatagory.getDataTypeName())) {
                    String errorCode = ErrorCodes.JCMP1001;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_TYPE_NAME);
                    errs.add(error);
                }
                if (ObjectUtils.isEmpty(dataCatagory.getDataItems())) {
                    String errorCode = ErrorCodes.JCMP1001;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_ITEMS);
                    errs.add(error);
                }
            }
        }
        if (ObjectUtils.isEmpty(request.getDetails())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.DETAILS);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getStatus())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.STATUS);
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateBusinessCreateRequest(BusinessApplicationRequest request, Map<String, String> headers) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (!headers.containsKey(Constants.SCOPE_LEVEL_HEADER)) {
            Map<String, Object> error = getHeaderErrorDetails(ErrorCodes.JCMP2001, Constants.SCOPE_LEVEL_HEADER);
            errs.add(error);
        }

        if (ObjectUtils.isEmpty(request.getName())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.NAME);
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateProcessorActivityUpdateRequest(ProcessorActivityRequest request) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (!ObjectUtils.isEmpty(request.getDataTypeList())) {
            for (DataCatagory dataCatagory : request.getDataTypeList()) {
                if (ObjectUtils.isEmpty(dataCatagory.getDataTypeId())) {
                    String errorCode = ErrorCodes.JCMP1001;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_TYPE_ID);
                    errs.add(error);
                }
                if (ObjectUtils.isEmpty(dataCatagory.getDataTypeName())) {
                    String errorCode = ErrorCodes.JCMP1001;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_TYPE_NAME);
                    errs.add(error);
                }
                if (ObjectUtils.isEmpty(dataCatagory.getDataItems())) {
                    String errorCode = ErrorCodes.JCMP1001;
                    Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_ITEMS);
                    errs.add(error);

                    if (!ObjectUtils.isEmpty(errs)) {
                        throw new BodyValidationException(errs);
                    }
                }
            }
        }
    }

    public void validateAuth(String authHeader) throws PartnerPortalException, ParseException, BodyValidationException {
        String token = authHeader.substring(7).trim();
        SignedJWT signedJWT = SignedJWT.parse(token);
        // Extract all claims as map
        Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();

        // Extract userId and tenantId from claims map
        String userId = claims.get("sub") != null ? claims.get("sub").toString() : null;
        String tenantId = claims.get("tenantId") != null ? claims.get("tenantId").toString() : null;
        ThreadContext.put("userId", userId);

        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(tenantId) || !this.tenantRepository.existsByTenantId(tenantId)) {
            Map<String, Object> error = getHeaderErrorDetails(ErrorCodes.JCMP2001, Constants.TENANT_ID_HEADER);
            errs.add(error);
        }
        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
        ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
    }

    public void validateNotificationCreateRequest(Map<String, String> headers, NotificationDetails notificationDetails) throws BodyValidationException {
        validateConfigHeader(headers);
        List<Map<String, Object>> errs = new ArrayList<>();

        if (ObjectUtils.isEmpty(notificationDetails.getBaseUrl())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, "baseUrl");
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(notificationDetails.getClientId())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, "clientId");
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(notificationDetails.getClientSecret())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, "clientSecret");
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(notificationDetails.getSid())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, "sid");
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateNotificationUpdateRequest(Map<String, String> headers, NotificationDetails notificationDetails) throws BodyValidationException {
        validateConfigHeader(headers);
        List<Map<String, Object>> errs = new ArrayList<>();

        if (ObjectUtils.isEmpty(notificationDetails.getBaseUrl())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, "baseUrl");
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(notificationDetails.getClientId())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, "clientId");
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(notificationDetails.getClientSecret())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, "clientSecret");
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(notificationDetails.getSid())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, "sid");
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateDigiLockerCredentialRequest(DigiLockerCredentialRequest request, Map<String, String> headers) throws BodyValidationException {
        validateConfigHeader(headers);
        List<Map<String, Object>> errs = new ArrayList<>();

        if (ObjectUtils.isEmpty(request.getClientId())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.CLIENT_ID);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getClientSecret())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.CLIENT_SECRET);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getRedirectUri())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.REDIRECT_URI);
            errs.add(error);
        }
        if (ObjectUtils.isEmpty(request.getCodeVerifier())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.CODE_VERIFIER);
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(new ArrayList<>(errs));
        }
    }

    public void validateDataBreachReportRequest(DataBreachReportRequest request) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();

        // Validate incidentDetails
        if (ObjectUtils.isEmpty(request.getIncidentDetails())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.INCIDENT_DETAILS);
            errs.add(error);
        } else {
            DataBreachReportRequest.IncidentDetails incidentDetails = request.getIncidentDetails();

            if (ObjectUtils.isEmpty(incidentDetails.getDiscoveryDateTime())) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.DISCOVERY_DATE_TIME);
                errs.add(error);
            }

            if (ObjectUtils.isEmpty(incidentDetails.getOccurrenceDateTime())) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.OCCURRENCE_DATE_TIME);
                errs.add(error);
            }

            if (ObjectUtils.isEmpty(incidentDetails.getBreachType())) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.BREACH_TYPE);
                errs.add(error);
            }

            if (ObjectUtils.isEmpty(incidentDetails.getBriefDescription()) ||
                    incidentDetails.getBriefDescription().trim().isEmpty()) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.BRIEF_DESCRIPTION);
                errs.add(error);
            }

            if (ObjectUtils.isEmpty(incidentDetails.getAffectedSystemOrService()) ||
                    incidentDetails.getAffectedSystemOrService().isEmpty()) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.AFFECTED_SYSTEM_OR_SERVICE);
                errs.add(error);
            }
        }

        // Validate dataInvolved
        if (ObjectUtils.isEmpty(request.getDataInvolved())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_INVOLVED);
            errs.add(error);
        } else {
            DataBreachReportRequest.DataInvolved dataInvolved = request.getDataInvolved();

            if (ObjectUtils.isEmpty(dataInvolved.getPersonalDataCategories()) ||
                    dataInvolved.getPersonalDataCategories().isEmpty()) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.PERSONAL_DATA_CATEGORIES);
                errs.add(error);
            }

            if (ObjectUtils.isEmpty(dataInvolved.getAffectedDataPrincipalsCount()) ||
                    dataInvolved.getAffectedDataPrincipalsCount() <= 0) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.AFFECTED_DATA_PRINCIPALS_COUNT);
                errs.add(error);
            }

            if (ObjectUtils.isEmpty(dataInvolved.getDataEncryptedOrProtected())) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.DATA_ENCRYPTED_OR_PROTECTED);
                errs.add(error);
            }

            if (ObjectUtils.isEmpty(dataInvolved.getPotentialImpactDescription()) ||
                    dataInvolved.getPotentialImpactDescription().trim().isEmpty()) {
                String errorCode = ErrorCodes.JCMP1001;
                Map<String, Object> error = getErrorDetails(errorCode, Constants.POTENTIAL_IMPACT_DESCRIPTION);
                errs.add(error);
            }
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    public void validateDataBreachUpdateRequest(DataBreachUpdateRequest request) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();

        if (ObjectUtils.isEmpty(request.getStatus())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.STATUS);
            errs.add(error);
        }

        if (ObjectUtils.isEmpty(request.getRemarks())) {
            String errorCode = ErrorCodes.JCMP1001;
            Map<String, Object> error = getErrorDetails(errorCode, Constants.REMARKS);
            errs.add(error);
        }

        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }
    

    public static void validateUploadFontRequest(UserDashboardFontRequest request) throws BodyValidationException{
    	List<Map<String, Object>> errs = new ArrayList<>();
    	if(request.getTypographySettings()==null || request.getTypographySettings().isEmpty()) {
    		String errorCode = ErrorCodes.JCMP1019;
    		Map<String, Object> error = getErrorDetails(errorCode, Constants.TYPOGRAPHY_SETTINGS);
    		errs.add(error);
    	}else {
    		for(Map.Entry<LANGUAGE, LanguageTypographySettings> entry: request.getTypographySettings().entrySet()) {
        		LanguageTypographySettings currSettings = request.getTypographySettings().get(entry.getKey());
        		if(currSettings.getFontFile()==null || currSettings.getFontFile().trim().equals("")) {
        			String errorCode = ErrorCodes.JCMP1020;
            		Map<String, Object> error = getErrorDetails(errorCode, Constants.TYPOGRAPHY_SETTINGS);
            		errs.add(error);
        		}else if(!Utils.isValidBase64(currSettings.getFontFile())){
        			String errorCode = ErrorCodes.JCMP1024;
            		Map<String, Object> error = getErrorDetails(errorCode, Constants.TYPOGRAPHY_SETTINGS);
            		errs.add(error);
        		}else {
        			String base64File = currSettings.getFontFile();
        			byte[] decodedBytes = Base64.getDecoder().decode(base64File);
        	        String mimeType = new Tika().detect(decodedBytes);
        	        if (!ALLOWED_FONT_TYPES.contains(mimeType)) {
        	        	String errorCode = ErrorCodes.JCMP1024;
        	    		Map<String, Object> error = getErrorDetails(errorCode, Constants.TYPOGRAPHY_SETTINGS);
        	    		errs.add(error);
        	        }
        			
        		}
        		if(currSettings.getFontSize()==null) {
        			String errorCode = ErrorCodes.JCMP1021;
            		Map<String, Object> error = getErrorDetails(errorCode, Constants.TYPOGRAPHY_SETTINGS);
            		errs.add(error);
        		}
        		if(currSettings.getFontWeight()==null) {
        			String errorCode = ErrorCodes.JCMP1022;
            		Map<String, Object> error = getErrorDetails(errorCode, Constants.TYPOGRAPHY_SETTINGS);
            		errs.add(error);
        		}
        		if(currSettings.getFontStyle()==null  || currSettings.getFontStyle().trim().equals("")) {
        			String errorCode = ErrorCodes.JCMP1023;
            		Map<String, Object> error = getErrorDetails(errorCode, Constants.TYPOGRAPHY_SETTINGS);
            		errs.add(error);
        		}
        	}
    	}
    	if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }
}
