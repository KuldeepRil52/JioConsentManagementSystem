package com.jio.digigov.notification.service.onboarding.provider;

import com.jio.digigov.notification.dto.onboarding.EmailTemplateConfig;
import com.jio.digigov.notification.dto.onboarding.EventTemplateDefinition;
import com.jio.digigov.notification.dto.onboarding.SmsTemplateConfig;
import com.jio.digigov.notification.dto.onboarding.json.TemplateDataFile;
import com.jio.digigov.notification.service.onboarding.EmailContentGenerator;
import com.jio.digigov.notification.service.onboarding.OnboardingDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides default notification template definitions for all event types.
 * Ported from create_templates.js script.
 *
 * This provider contains 16 event template definitions (32 templates total: 16 SMS + 16 Email).
 * Note: Retention policy events (DATA_RETENTION_DURATION_EXPIRED, LOG_RETENTION_DURATION_EXPIRED)
 * do not have user-facing templates as they only notify DPO via callback.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingTemplateProvider {

    private final OnboardingDataLoader dataLoader;
    private final EmailContentGenerator contentGenerator;
    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a random 10-digit DLT template ID for SMS templates.
     * Matches the pattern from create_templates.js.
     *
     * @return 10-digit DLT template ID as String
     */
    private String generateDltTemplateId() {
        long min = 1000000000L;
        long max = 9999999999L;
        long dltId = min + ((long) (random.nextDouble() * (max - min)));
        return String.valueOf(dltId);
    }

    /**
     * Creates a professionally styled HTML email template wrapper.
     * Provides consistent branding, responsive design, and accessibility.
     *
     * @param title Email title/heading
     * @param content Main content HTML (can include paragraphs, lists, etc.)
     * @param footerText Optional footer text
     * @return Complete HTML email template with styling
     */
    private String createStyledEmailTemplate(String title, String content, String footerText) {
        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>" + title + "</title>" +
                "</head>" +
                "<body style=\"margin:0;padding:0;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;background-color:#f4f6f9;\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f4f6f9;padding:20px 0;\">" +
                "<tr><td align=\"center\">" +
                "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"max-width:600px;background-color:#ffffff;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);\">" +
                // Header with brand color
                "<tr><td style=\"background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:30px;text-align:center;border-radius:8px 8px 0 0;\">" +
                "<h1 style=\"margin:0;color:#ffffff;font-size:24px;font-weight:600;\">" + title + "</h1>" +
                "</td></tr>" +
                // Content area
                "<tr><td style=\"padding:40px 30px;color:#333333;font-size:15px;line-height:1.6;\">" +
                content +
                "</td></tr>" +
                // Footer
                "<tr><td style=\"background-color:#f8f9fa;padding:20px 30px;text-align:center;border-radius:0 0 8px 8px;border-top:1px solid #e9ecef;\">" +
                "<p style=\"margin:0;font-size:13px;color:#6c757d;\">" + footerText + "</p>" +
                "<p style=\"margin:10px 0 0 0;font-size:12px;color:#adb5bd;\">This is an automated notification. Please do not reply to this email.</p>" +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body></html>";
    }

    /**
     * Creates a styled information box for key details.
     *
     * @param content Content to display in the box
     * @return HTML for styled info box
     */
    private String createInfoBox(String content) {
        return "<div style=\"background-color:#e7f3ff;border-left:4px solid#667eea;padding:15px;margin:20px 0;border-radius:4px;\">" +
                content +
                "</div>";
    }

    /**
     * Creates a styled button/link.
     *
     * @param text Button text
     * @param url Link URL (use placeholder like <#ARG5>)
     * @return HTML for styled button
     */
    private String createButton(String text, String url) {
        return "<div style=\"text-align:center;margin:30px 0;\">" +
                "<a href=\"" + url + "\" style=\"display:inline-block;padding:12px 30px;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:#ffffff;text-decoration:none;border-radius:5px;font-weight:600;font-size:14px;\">" +
                text +
                "</a></div>";
    }

    /**
     * Returns all default event template definitions.
     *
     * @return List of EventTemplateDefinition objects
     */
    /**
     * Loads template definitions from JSON files.
     * This is the NEW approach - data from JSON, styling from code.
     *
     * @return List of EventTemplateDefinition objects
     */
    public List<EventTemplateDefinition> getTemplateDefinitionsFromJson() {
        if (!dataLoader.isDataLoaded()) {
            log.warn("Onboarding data not loaded, falling back to hardcoded approach");
            return getAllTemplateDefinitions();
        }

        log.info("Loading template definitions from JSON data...");
        List<EventTemplateDefinition> templates = new ArrayList<>();
        List<TemplateDataFile> templateDataList = dataLoader.getAllTemplates();

        for (TemplateDataFile templateData : templateDataList) {
            try {
                EventTemplateDefinition template = convertTemplateData(templateData);
                templates.add(template);
            } catch (Exception e) {
                log.error("Failed to convert template for {}/{}",
                        templateData.getEventType(), templateData.getRecipientType(), e);
            }
        }

        log.info("Loaded {} template definitions from JSON data", templates.size());
        return templates;
    }

    /**
     * Converts template data from JSON to EventTemplateDefinition.
     */
    private EventTemplateDefinition convertTemplateData(TemplateDataFile templateData) {
        EventTemplateDefinition.EventTemplateDefinitionBuilder builder = EventTemplateDefinition.builder()
                .eventType(templateData.getEventType())
                .recipientType(templateData.getRecipientType());

        // Convert SMS if present
        if (templateData.getSms() != null) {
            builder.sms(convertSmsData(templateData.getSms()));
        }

        // Convert Email if present
        if (templateData.getEmail() != null) {
            builder.email(convertEmailData(
                    templateData.getEmail(),
                    templateData.getEventType(),
                    templateData.getRecipientType()
            ));
        }

        return builder.build();
    }

    /**
     * Converts SMS data from JSON.
     */
    private SmsTemplateConfig convertSmsData(TemplateDataFile.SmsData smsData) {
        return SmsTemplateConfig.builder()
                .language("english")
                .template(smsData.getTemplate())
                .whiteListedNumbers(List.of()) // Will be populated during onboarding
                .templateDetails(extractTemplateDetails(smsData.getTemplate()))
                .oprCountries(List.of("IN"))
                .dltEntityId("1001")
                .dltTemplateId(generateDltTemplateId())
                .from("JioGCS-S")
                .argumentsMap(smsData.getArguments() != null ? smsData.getArguments() : new HashMap<>())
                .build();
    }

    /**
     * Converts Email data from JSON with generated HTML content.
     * Uses priority-based content generation:
     * 1. Complete HTML from JSON (if available)
     * 2. Component-based generation (if configured)
     * 3. Hardcoded templates (fallback)
     */
    private EmailTemplateConfig convertEmailData(
            TemplateDataFile.EmailData emailData,
            String eventType,
            String recipientType) {

        // Generate styled HTML content using priority-based approach
        String htmlContent = contentGenerator.generateEmailContent(emailData, eventType, recipientType);

        return EmailTemplateConfig.builder()
                .language("english")
                .toRecipients(List.of()) // Will be populated during onboarding
                .ccRecipients(List.of())
                .templateDetails(extractTemplateDetails(emailData.getSubject()))
                .templateSubject(emailData.getSubject())
                .templateBody(htmlContent)
                .templateFromName(emailData.getFromName() != null ? emailData.getFromName() : "DPDP Notification System")
                .emailType("HTML")
                .argumentsSubjectMap(emailData.getSubjectArguments() != null ? emailData.getSubjectArguments() : new HashMap<>())
                .argumentsBodyMap(emailData.getBodyArguments() != null ? emailData.getBodyArguments() : new HashMap<>())
                .build();
    }

    /**
     * Extracts template details from template text (max 50 chars).
     */
    private String extractTemplateDetails(String text) {
        if (text == null || text.isEmpty()) {
            return "Notification template";
        }

        // Remove placeholders and clean up
        String cleaned = text.replaceAll("<#ARG\\d+>", "").trim();

        // Limit to 47 characters to stay under 50 char limit
        if (cleaned.length() > 47) {
            cleaned = cleaned.substring(0, 44) + "...";
        }

        return cleaned;
    }

    /**
     * Gets all template definitions using the LEGACY hardcoded approach.
     * This method is kept for backward compatibility and fallback.
     *
     * @return List of EventTemplateDefinition objects
     */
    public List<EventTemplateDefinition> getAllTemplateDefinitions() {
        log.debug("Retrieving all template definitions (legacy hardcoded approach)");

        List<EventTemplateDefinition> templates = new ArrayList<>();

        // Consent Events (7)
        templates.add(createConsentRequestPendingTemplate());
        templates.add(createConsentRenewalRequestTemplate());
        templates.add(createConsentCreatedTemplate());
        templates.add(createConsentUpdatedTemplate());
        templates.add(createConsentWithdrawnTemplate());
        templates.add(createConsentExpiredTemplate());
        templates.add(createConsentRenewedTemplate());

        // Grievance Events (7)
        templates.add(createGrievanceRaisedTemplate());
        templates.add(createGrievanceInprocessTemplate());
        templates.add(createGrievanceEscalatedTemplate());
        templates.add(createGrievanceResolvedTemplate());
        templates.add(createGrievanceDeniedTemplate());
        templates.add(createGrievanceClosedTemplate());
        templates.add(createGrievanceStatusUpdatedTemplate());

        // Data Events (3)
        templates.add(createDataDeletedTemplate());
        templates.add(createDataSharedTemplate());
        templates.add(createDataBreachedTemplate());

        // DPO Email Templates for Grievance Events (7)
        templates.add(createGrievanceRaisedDpoTemplate());
        templates.add(createGrievanceInprocessDpoTemplate());
        templates.add(createGrievanceEscalatedDpoTemplate());
        templates.add(createGrievanceResolvedDpoTemplate());
        templates.add(createGrievanceDeniedDpoTemplate());
        templates.add(createGrievanceClosedDpoTemplate());
        templates.add(createGrievanceStatusUpdatedDpoTemplate());

        log.info("Loaded {} event template definitions ({} total templates: SMS + Email)",
                templates.size(), templates.size() * 2);

        return templates;
    }

    // ==================== CONSENT EVENTS ====================

    private EventTemplateDefinition createConsentRequestPendingTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("CONSENT_REQUEST_PENDING")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your consent request (ID: <#ARG2>) with <#ARG3> is pending approval. Created on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("CONSENT_REQUEST_PENDING SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_CREATED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("CONSENT_REQUEST_PENDING email notification")
                        .templateSubject("Consent Request Pending – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Consent Request Pending",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your consent request has been submitted and is currently pending approval.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Submitted On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#ffc107;color:#000000;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">PENDING</span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">We're reviewing your consent request. You'll be notified once a decision has been made.</p>" +
                        createButton("Review Request", "<#ARG5>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">Thank you for managing your data privacy preferences with us.</p>",
                        "We'll notify you as soon as your consent request is processed."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_CREATED_DATE",
                                "<#ARG5>", "MASTER_LABEL_REVIEW_URL"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createConsentRenewalRequestTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("CONSENT_RENEWAL_REQUEST")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your consent (ID: <#ARG2>) with <#ARG3> requires renewal. Expires on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("CONSENT_RENEWAL_REQUEST SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_EXPIRY_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("CONSENT_RENEWAL_REQUEST email notification")
                        .templateSubject("Consent Renewal Required – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Consent Renewal Required",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your consent is approaching its expiration date and requires renewal to continue.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Expires On:</strong> <span style=\"display:inline-block;background-color:#dc3545;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\"><#ARG4></span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">Please renew your consent before the expiration date to ensure uninterrupted service.</p>" +
                        createButton("Renew Consent", "<#ARG5>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">If you choose not to renew, your consent will expire and related services may be affected.</p>",
                        "Thank you for managing your data privacy preferences."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_EXPIRY_DATE",
                                "<#ARG5>", "MASTER_LABEL_RENEWAL_URL"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createConsentCreatedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("CONSENT_CREATED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your consent (ID: <#ARG2>) with <#ARG3> was successfully created on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("CONSENT_CREATED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_CREATED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("CONSENT_CREATED email notification")
                        .templateSubject("Consent Created – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Consent Successfully Created",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your consent has been successfully created and is now active.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Created On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#28a745;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">ACTIVE</span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">You can view and manage your consent details at any time using the link below.</p>" +
                        createButton("View Consent Details", "<#ARG5>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">Thank you for trusting us with your data. You can withdraw your consent at any time.</p>",
                        "We value your privacy and will handle your data responsibly."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_CREATED_DATE",
                                "<#ARG5>", "MASTER_LABEL_REVIEW_URL"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createConsentUpdatedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("CONSENT_UPDATED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your consent (ID: <#ARG2>) with <#ARG3> was updated on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("CONSENT_UPDATED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_UPDATED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("CONSENT_UPDATED email notification")
                        .templateSubject("Consent Updated – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Consent Updated",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your consent details have been updated. Please review the changes to ensure they reflect your preferences.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Updated On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#17a2b8;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">UPDATED</span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">Review the updated consent terms to ensure everything is accurate.</p>" +
                        createButton("Review Changes", "<#ARG5>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">If you didn't make this change, please contact our support team immediately.</p>",
                        "Your data privacy preferences have been updated successfully."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_UPDATED_DATE",
                                "<#ARG5>", "MASTER_LABEL_REVIEW_URL"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createConsentWithdrawnTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("CONSENT_WITHDRAWN")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your consent (ID: <#ARG2>) with <#ARG3> was withdrawn on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("CONSENT_WITHDRAWN SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_WITHDRAWN_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("CONSENT_WITHDRAWN email notification")
                        .templateSubject("Consent Withdrawn – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Consent Withdrawn",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your consent has been successfully withdrawn. We have stopped processing your data as requested.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Withdrawn On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#6c757d;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">WITHDRAWN</span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">Your data will no longer be processed under this consent. Any ongoing data processing activities have been halted.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">You can provide consent again at any time if you change your mind. Thank you for using our services.</p>",
                        "We respect your privacy choices and have processed your withdrawal request."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_WITHDRAWN_DATE"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createConsentExpiredTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("CONSENT_EXPIRED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your consent (ID: <#ARG2>) with <#ARG3> has expired on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("CONSENT_EXPIRED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_EXPIRY_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("CONSENT_EXPIRED email notification")
                        .templateSubject("Consent Expired – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Consent Expired",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your consent has expired and is no longer active. Data processing under this consent has been stopped.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Expired On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#dc3545;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">EXPIRED</span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">If you wish to continue using our services, please renew your consent using the button below.</p>" +
                        createButton("Renew Consent", "<#ARG5>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">Renewing your consent will allow us to resume data processing activities.</p>",
                        "Thank you for your attention to your privacy preferences."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_EXPIRY_DATE",
                                "<#ARG5>", "MASTER_LABEL_RENEWAL_URL"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createConsentRenewedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("CONSENT_RENEWED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your consent (ID: <#ARG2>) with <#ARG3> was renewed on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("CONSENT_RENEWED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_RENEWAL_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("CONSENT_RENEWED email notification")
                        .templateSubject("Consent Renewed – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Consent Successfully Renewed",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Great news! Your consent has been successfully renewed and is now active again.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Renewed On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#28a745;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">ACTIVE</span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">Your consent is now active and data processing can continue. You can view the updated consent details anytime.</p>" +
                        createButton("View Updated Consent", "<#ARG5>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">Thank you for continuing to trust us with your data. You can withdraw your consent at any time.</p>",
                        "We appreciate your continued trust in our data handling practices."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_RENEWAL_DATE",
                                "<#ARG5>", "MASTER_LABEL_REVIEW_URL"
                        ))
                        .build())
                .build();
    }

    // ==================== GRIEVANCE EVENTS ====================

    private EventTemplateDefinition createGrievanceRaisedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_RAISED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your grievance (ID: <#ARG2>) has been raised with <#ARG3> on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("GRIEVANCE_RAISED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_CREATED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("GRIEVANCE_RAISED email notification")
                        .templateSubject("Grievance Raised – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Grievance Successfully Raised",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your grievance has been successfully raised and is now being processed by our team.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Raised On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Current Status:</strong> <span style=\"display:inline-block;background-color:#28a745;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\"><#ARG5></span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">Our team will review your grievance and respond within 3-5 business days. You can track the progress of your grievance anytime using the link below.</p>" +
                        createButton("Track Grievance", "<#ARG6>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">If you have any questions, please don't hesitate to contact our support team.</p>",
                        "Thank you for bringing this to our attention. We are committed to resolving your grievance."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_CREATED_DATE",
                                "<#ARG5>", "MASTER_LABEL_STATUS",
                                "<#ARG6>", "MASTER_LABEL_TRACKING_URL"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceInprocessTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_INPROCESS")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your grievance (ID: <#ARG2>) with <#ARG3> is now being processed.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("GRIEVANCE_INPROCESS SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("GRIEVANCE_INPROCESS email notification")
                        .templateSubject("Grievance In Process – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Grievance In Process",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Good news! Your grievance is now being actively processed by our team.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Current Status:</strong> <span style=\"display:inline-block;background-color:#ffc107;color:#000000;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\"><#ARG4></span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">Our team is currently reviewing your case and working towards a resolution. We'll keep you updated on any progress.</p>" +
                        createButton("Track Progress", "<#ARG5>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">Thank you for your patience as we work to address your concerns.</p>",
                        "We appreciate your patience and will update you soon."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_STATUS",
                                "<#ARG5>", "MASTER_LABEL_TRACKING_URL"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceEscalatedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_ESCALATED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your grievance (ID: <#ARG2>) with <#ARG3> has been escalated on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("GRIEVANCE_ESCALATED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_ESCALATED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("GRIEVANCE_ESCALATED email notification")
                        .templateSubject("Grievance Escalated – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Grievance Escalated",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your grievance has been escalated to higher authorities for immediate attention and priority resolution.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Escalated On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Current Status:</strong> <span style=\"display:inline-block;background-color:#fd7e14;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\"><#ARG5></span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">This escalation ensures that your case receives the highest priority. Senior team members will review your grievance and work expeditiously to resolve it.</p>" +
                        createButton("Track Grievance", "<#ARG6>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">We understand the importance of your concern and are committed to a swift resolution.</p>",
                        "Your grievance is being handled with the highest priority."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_ESCALATED_DATE",
                                "<#ARG5>", "MASTER_LABEL_STATUS",
                                "<#ARG6>", "MASTER_LABEL_TRACKING_URL"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceResolvedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_RESOLVED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your grievance (ID: <#ARG2>) with <#ARG3> has been resolved on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("GRIEVANCE_RESOLVED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_RESOLVED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("GRIEVANCE_RESOLVED email notification")
                        .templateSubject("Grievance Resolved – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Grievance Resolved",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">We are pleased to inform you that your grievance has been successfully resolved.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Resolved On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#28a745;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">RESOLVED</span></p>"
                        ) +
                        "<div style=\"background-color:#f8f9fa;border-left:4px solid #28a745;padding:15px;margin:20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0 0 5px 0;font-weight:600;color:#28a745;\">Resolution Details:</p>" +
                        "<p style=\"margin:0;color:#333333;\"><#ARG5></p>" +
                        "</div>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">Thank you for bringing this matter to our attention. We value your feedback.</p>",
                        "We appreciate your patience and hope this resolution meets your expectations."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_RESOLVED_DATE",
                                "<#ARG5>", "MASTER_LABEL_RESOLUTION_DETAILS"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceDeniedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_DENIED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your grievance (ID: <#ARG2>) with <#ARG3> has been denied on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("GRIEVANCE_DENIED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_DENIED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("GRIEVANCE_DENIED email notification")
                        .templateSubject("Grievance Denied – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Grievance Denied",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">After careful review, we regret to inform you that your grievance has been denied.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Denied On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#dc3545;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">DENIED</span></p>"
                        ) +
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0 0 5px 0;font-weight:600;color:#856404;\">Reason for Denial:</p>" +
                        "<p style=\"margin:0;color:#333333;\"><#ARG5></p>" +
                        "</div>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">If you have additional information or wish to discuss this decision, please contact our support team.</p>",
                        "We appreciate your understanding. Feel free to reach out with any questions."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_DENIED_DATE",
                                "<#ARG5>", "MASTER_LABEL_DENIAL_REASON"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceClosedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_CLOSED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your grievance (ID: <#ARG2>) with <#ARG3> has been closed on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("GRIEVANCE_CLOSED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_CLOSED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("GRIEVANCE_CLOSED email notification")
                        .templateSubject("Grievance Closed – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Grievance Closed",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your grievance has been officially closed. This case is now complete.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Closed On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#6c757d;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">CLOSED</span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">Thank you for using our grievance redressal system. Your feedback helps us improve our services.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">If you have any further concerns, please don't hesitate to raise a new grievance.</p>",
                        "Thank you for giving us the opportunity to address your concerns."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_CLOSED_DATE"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceStatusUpdatedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_STATUS_UPDATED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your grievance (ID: <#ARG2>) status with <#ARG3> has been updated on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("GRIEVANCE_STATUS_UPDATED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_UPDATED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("GRIEVANCE_STATUS_UPDATED email notification")
                        .templateSubject("Grievance Status Updated – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Grievance Status Updated",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">The status of your grievance has been updated. Here are the latest details:</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Updated On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">New Status:</strong> <span style=\"display:inline-block;background-color:#17a2b8;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\"><#ARG5></span></p>"
                        ) +
                        "<p style=\"margin:20px 0;\">You can continue to track the progress of your grievance using the link below.</p>" +
                        createButton("Track Grievance", "<#ARG6>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">We'll notify you of any further updates on your case.</p>",
                        "Thank you for your patience as we work to resolve your grievance."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_ORGANIZATION_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_ORGANIZATION_NAME",
                                "<#ARG4>", "MASTER_LABEL_UPDATED_DATE",
                                "<#ARG5>", "MASTER_LABEL_STATUS",
                                "<#ARG6>", "MASTER_LABEL_TRACKING_URL"
                        ))
                        .build())
                .build();
    }

    // ==================== DATA EVENTS ====================

    private EventTemplateDefinition createDataDeletedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("DATA_DELETED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your data (ID: <#ARG2>) with <#ARG3> has been deleted on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("DATA_DELETED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_DATA_REF_ID",
                                "<#ARG3>", "MASTER_LABEL_DATA_FIDUCIARY_NAME",
                                "<#ARG4>", "MASTER_LABEL_DELETED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("DATA_DELETED email notification")
                        .templateSubject("Data Deleted – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Data Permanently Deleted",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your data has been permanently deleted from our systems as requested. This action is irreversible.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Data Reference ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Data Fiduciary:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Deleted On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#dc3545;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">PERMANENTLY DELETED</span></p>"
                        ) +
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#856404;\">⚠️ Important Notice</p>" +
                        "<p style=\"margin:5px 0 0 0;color:#333333;\">This action cannot be undone. All associated data has been permanently removed from our databases and cannot be recovered.</p>" +
                        "</div>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">We respect your right to data deletion. If you have any questions, please contact our support team.</p>",
                        "Your data privacy rights have been honored. Thank you for your trust."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_DATA_FIDUCIARY_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_DATA_REF_ID",
                                "<#ARG3>", "MASTER_LABEL_DATA_FIDUCIARY_NAME",
                                "<#ARG4>", "MASTER_LABEL_DELETED_DATE"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createDataSharedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("DATA_SHARED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear user (<#ARG1>), your data (ID: <#ARG2>) has been shared with <#ARG3> on <#ARG4>.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("DATA_SHARED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_DATA_REF_ID",
                                "<#ARG3>", "MASTER_LABEL_SHARED_WITH",
                                "<#ARG4>", "MASTER_LABEL_SHARED_DATE"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("DATA_SHARED email notification")
                        .templateSubject("Data Shared – <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "Data Shared with Third Party",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">Your data has been shared with a third party as part of our data processing activities. Here are the details:</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Data Reference ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Shared With:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Shared On:</strong> <#ARG4></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Third Party Name:</strong> <#ARG5></p>"
                        ) +
                        "<div style=\"background-color:#e7f3ff;border-left:4px solid #007bff;padding:15px;margin:20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#004085;\">🔒 Privacy Information</p>" +
                        "<p style=\"margin:5px 0 0 0;color:#333333;\">Your data has been shared in accordance with your consent and our privacy policy. The third party is bound by data protection obligations.</p>" +
                        "</div>" +
                        "<p style=\"margin:20px 0;\">You can view complete sharing details and manage your data preferences anytime.</p>" +
                        createButton("View Sharing Details", "<#ARG6>") +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">If you have concerns about this data sharing, please contact our privacy team or withdraw your consent.</p>",
                        "We are committed to transparency in how your data is shared and used."
                ))
                        .templateFromName("JioGCS Support")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_DATA_FIDUCIARY_NAME"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_DATA_REF_ID",
                                "<#ARG3>", "MASTER_LABEL_SHARED_WITH",
                                "<#ARG4>", "MASTER_LABEL_SHARED_DATE",
                                "<#ARG5>", "MASTER_LABEL_THIRD_PARTY_NAME",
                                "<#ARG6>", "MASTER_LABEL_TRACKING_URL"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createDataBreachedTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("DATA_BREACHED")
                .sms(SmsTemplateConfig.builder()
                        .language("english")
                        .template("Dear <#ARG1>, your data (Consent ID: <#ARG2>), shared with <#ARG3>, is breached.")
                        .whiteListedNumbers(List.of("8369467086"))
                        .templateDetails("DATA_BREACHED SMS notification")
                        .oprCountries(List.of("IN"))
                        .dltEntityId("1001")
                        .dltTemplateId(generateDltTemplateId())
                        .from("JioGCS-S")
                        .argumentsMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_DATA_FIDUCIARY_NAME"
                        ))
                        .build())
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("user@example.com"))
                        .ccRecipients(List.of("admin@example.com"))
                        .templateDetails("DATA_BREACHED email notification")
                        .templateSubject("Security Alert - Data Breach Notification")
                        .templateBody(createStyledEmailTemplate(
                        "Security Alert: Data Breach Detected",
                        "<p style=\"margin:0 0 15px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                        "<p style=\"margin:0 0 20px 0;\">We are writing to inform you of a data breach incident involving your personal data. Your privacy and security are our top priorities, and we want to ensure you are fully informed.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">User:</strong> <#ARG1></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Data Shared With:</strong> <#ARG3></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Status:</strong> <span style=\"display:inline-block;background-color:#dc3545;color:#ffffff;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;\">BREACH DETECTED</span></p>"
                        ) +
                        "<div style=\"background-color:#f8d7da;border-left:4px solid #dc3545;padding:15px;margin:20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#721c24;\">⚠️ Immediate Action Required</p>" +
                        "<p style=\"margin:5px 0 0 0;color:#721c24;\">A security breach has been detected involving your personal data that was shared with <#ARG3>. We recommend you take immediate steps to protect your account and personal information.</p>" +
                        "</div>" +
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#856404;\">Recommended Actions:</p>" +
                        "<ul style=\"margin:10px 0 0 0;padding-left:20px;color:#333333;\">" +
                        "<li style=\"margin:5px 0;\">Review your account activity and consent settings</li>" +
                        "<li style=\"margin:5px 0;\">Change your passwords and enable two-factor authentication</li>" +
                        "<li style=\"margin:5px 0;\">Monitor your accounts for any suspicious activity</li>" +
                        "<li style=\"margin:5px 0;\">Contact our security team if you notice any unauthorized access</li>" +
                        "</ul>" +
                        "</div>" +
                        "<p style=\"margin:20px 0;\">We are conducting a thorough investigation and will take all necessary measures to prevent future incidents. Your data protection is our highest priority.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">If you have any questions or concerns about this breach, please contact our security team immediately. We are here to support you.</p>",
                        "We sincerely apologize for this incident and are committed to protecting your data."
                ))
                        .templateFromName("JioGCS Security")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of())
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_USER_IDENTIFIER",
                                "<#ARG2>", "MASTER_LABEL_CONSENT_ID",
                                "<#ARG3>", "MASTER_LABEL_DATA_FIDUCIARY_NAME"
                        ))
                        .build())
                .build();
    }

    // ==================== DPO EMAIL TEMPLATES FOR GRIEVANCE EVENTS ====================

    private EventTemplateDefinition createGrievanceRaisedDpoTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_RAISED")
                .recipientType("DATA_PROTECTION_OFFICER")
                .sms(null)
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("dpo@example.com"))
                        .ccRecipients(List.of())
                        .templateDetails("GRIEVANCE_RAISED DPO email notification")
                        .templateSubject("[DPO Alert] New Grievance Raised - Business: <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "DPO Alert: Grievance Event",
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert</p>" +
                        "</div>" +
                        "<p style=\"margin:0 0 20px 0;\">A grievance event has occurred that requires your attention and oversight.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                        ) +
                        "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                        "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                        "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                        "</div>" +
                        "<p style=\"margin:20px 0;\">Please review this grievance and take appropriate action in accordance with data protection regulations.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>",
                        "Your oversight ensures compliance with data protection requirements."
                ))
                        .templateFromName("DPDP Notification System")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_EVENT_TYPE",
                                "<#ARG4>", "MASTER_LABEL_JWT_TOKEN"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceInprocessDpoTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_INPROCESS")
                .recipientType("DATA_PROTECTION_OFFICER")
                .sms(null)
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("dpo@example.com"))
                        .ccRecipients(List.of())
                        .templateDetails("GRIEVANCE_INPROCESS DPO email notification")
                        .templateSubject("[DPO Alert] Grievance In Process - Business: <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "DPO Alert: Grievance Event",
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert</p>" +
                        "</div>" +
                        "<p style=\"margin:0 0 20px 0;\">A grievance event has occurred that requires your attention and oversight.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                        ) +
                        "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                        "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                        "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                        "</div>" +
                        "<p style=\"margin:20px 0;\">Please review this grievance and take appropriate action in accordance with data protection regulations.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>",
                        "Your oversight ensures compliance with data protection requirements."
                ))
                        .templateFromName("DPDP Notification System")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_EVENT_TYPE",
                                "<#ARG4>", "MASTER_LABEL_JWT_TOKEN"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceEscalatedDpoTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_ESCALATED")
                .recipientType("DATA_PROTECTION_OFFICER")
                .sms(null)
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("dpo@example.com"))
                        .ccRecipients(List.of())
                        .templateDetails("GRIEVANCE_ESCALATED DPO email notification")
                        .templateSubject("[DPO Alert] Grievance Escalated - Business: <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "DPO Alert: Grievance Event",
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert</p>" +
                        "</div>" +
                        "<p style=\"margin:0 0 20px 0;\">A grievance event has occurred that requires your attention and oversight.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                        ) +
                        "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                        "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                        "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                        "</div>" +
                        "<p style=\"margin:20px 0;\">Please review this grievance and take appropriate action in accordance with data protection regulations.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>",
                        "Your oversight ensures compliance with data protection requirements."
                ))
                        .templateFromName("DPDP Notification System")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_EVENT_TYPE",
                                "<#ARG4>", "MASTER_LABEL_JWT_TOKEN"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceResolvedDpoTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_RESOLVED")
                .recipientType("DATA_PROTECTION_OFFICER")
                .sms(null)
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("dpo@example.com"))
                        .ccRecipients(List.of())
                        .templateDetails("GRIEVANCE_RESOLVED DPO email notification")
                        .templateSubject("[DPO Alert] Grievance Resolved - Business: <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "DPO Alert: Grievance Event",
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert</p>" +
                        "</div>" +
                        "<p style=\"margin:0 0 20px 0;\">A grievance event has occurred that requires your attention and oversight.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                        ) +
                        "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                        "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                        "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                        "</div>" +
                        "<p style=\"margin:20px 0;\">Please review this grievance and take appropriate action in accordance with data protection regulations.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>",
                        "Your oversight ensures compliance with data protection requirements."
                ))
                        .templateFromName("DPDP Notification System")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_EVENT_TYPE",
                                "<#ARG4>", "MASTER_LABEL_JWT_TOKEN"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceDeniedDpoTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_DENIED")
                .recipientType("DATA_PROTECTION_OFFICER")
                .sms(null)
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("dpo@example.com"))
                        .ccRecipients(List.of())
                        .templateDetails("GRIEVANCE_DENIED DPO email notification")
                        .templateSubject("[DPO Alert] Grievance Denied - Business: <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "DPO Alert: Grievance Event",
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert</p>" +
                        "</div>" +
                        "<p style=\"margin:0 0 20px 0;\">A grievance event has occurred that requires your attention and oversight.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                        ) +
                        "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                        "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                        "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                        "</div>" +
                        "<p style=\"margin:20px 0;\">Please review this grievance and take appropriate action in accordance with data protection regulations.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>",
                        "Your oversight ensures compliance with data protection requirements."
                ))
                        .templateFromName("DPDP Notification System")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_EVENT_TYPE",
                                "<#ARG4>", "MASTER_LABEL_JWT_TOKEN"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceClosedDpoTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_CLOSED")
                .recipientType("DATA_PROTECTION_OFFICER")
                .sms(null)
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("dpo@example.com"))
                        .ccRecipients(List.of())
                        .templateDetails("GRIEVANCE_CLOSED DPO email notification")
                        .templateSubject("[DPO Alert] Grievance Closed - Business: <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "DPO Alert: Grievance Event",
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert</p>" +
                        "</div>" +
                        "<p style=\"margin:0 0 20px 0;\">A grievance event has occurred that requires your attention and oversight.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                        ) +
                        "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                        "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                        "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                        "</div>" +
                        "<p style=\"margin:20px 0;\">Please review this grievance and take appropriate action in accordance with data protection regulations.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>",
                        "Your oversight ensures compliance with data protection requirements."
                ))
                        .templateFromName("DPDP Notification System")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_EVENT_TYPE",
                                "<#ARG4>", "MASTER_LABEL_JWT_TOKEN"
                        ))
                        .build())
                .build();
    }

    private EventTemplateDefinition createGrievanceStatusUpdatedDpoTemplate() {
        return EventTemplateDefinition.builder()
                .eventType("GRIEVANCE_STATUS_UPDATED")
                .recipientType("DATA_PROTECTION_OFFICER")
                .sms(null)
                .email(EmailTemplateConfig.builder()
                        .language("english")
                        .toRecipients(List.of("dpo@example.com"))
                        .ccRecipients(List.of())
                        .templateDetails("GRIEVANCE_STATUS_UPDATED DPO email notification")
                        .templateSubject("[DPO Alert] Grievance Status Updated - Business: <#ARG1>")
                        .templateBody(createStyledEmailTemplate(
                        "DPO Alert: Grievance Event",
                        "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                        "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert</p>" +
                        "</div>" +
                        "<p style=\"margin:0 0 20px 0;\">A grievance event has occurred that requires your attention and oversight.</p>" +
                        createInfoBox(
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                            "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                            "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                        ) +
                        "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                        "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                        "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                        "</div>" +
                        "<p style=\"margin:20px 0;\">Please review this grievance and take appropriate action in accordance with data protection regulations.</p>" +
                        "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>",
                        "Your oversight ensures compliance with data protection requirements."
                ))
                        .templateFromName("DPDP Notification System")
                        .emailType("HTML")
                        .argumentsSubjectMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID"
                        ))
                        .argumentsBodyMap(Map.of(
                                "<#ARG1>", "MASTER_LABEL_BUSINESS_ID",
                                "<#ARG2>", "MASTER_LABEL_GRIEVANCE_ID",
                                "<#ARG3>", "MASTER_LABEL_EVENT_TYPE",
                                "<#ARG4>", "MASTER_LABEL_JWT_TOKEN"
                        ))
                        .build())
                .build();
    }

}
