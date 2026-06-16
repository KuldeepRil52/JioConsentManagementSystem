package com.jio.digigov.notification.service.onboarding;

import com.jio.digigov.notification.dto.onboarding.json.TemplateDataFile;
import com.jio.digigov.notification.service.templates.TemplateHtmlRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to generate styled HTML email content for different event types and recipients.
 *
 * <p>This service supports three priority levels for content generation:
 * <ol>
 *   <li><strong>Priority 1</strong>: Complete HTML from JSON (if completeHtmlContent provided)</li>
 *   <li><strong>Priority 2</strong>: Component-based generation from JSON (if components config provided)</li>
 *   <li><strong>Priority 3</strong>: Hardcoded templates (fallback for backward compatibility)</li>
 * </ol>
 * </p>
 *
 * @author DPDP Notification Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailContentGenerator {

    private final TemplateHtmlRenderer htmlRenderer;

    /**
     * Generates email HTML content with priority-based approach.
     *
     * <p><strong>Priority 1</strong>: Use completeHtmlContent if available
     * <br><strong>Priority 2</strong>: Generate from components config if available
     * <br><strong>Priority 3</strong>: Fall back to hardcoded template</p>
     *
     * @param emailData Email data from JSON
     * @param eventType Event type (for fallback)
     * @param recipientType Recipient type (for fallback)
     * @return Styled HTML email content
     */
    public String generateEmailContent(TemplateDataFile.EmailData emailData, String eventType, String recipientType) {
        // PRIORITY 1: Complete HTML content from JSON
        if (emailData.getCompleteHtmlContent() != null && !emailData.getCompleteHtmlContent().trim().isEmpty()) {
            log.debug("Using complete HTML content from JSON for {} - {}", eventType, recipientType);
            return emailData.getCompleteHtmlContent();
        }

        // PRIORITY 2: Component-based generation from JSON
        if (emailData.getComponents() != null) {
            log.debug("Using component-based generation for {} - {}", eventType, recipientType);
            return generateFromComponents(emailData.getComponents());
        }

        // PRIORITY 3: Hardcoded template fallback
        log.debug("Using hardcoded template for {} - {}", eventType, recipientType);
        return generateEmailContent(eventType, recipientType);
    }

    /**
     * Generates HTML content from component configuration (Priority 2).
     *
     * @param components Component configuration from JSON
     * @return Generated HTML email content
     */
    private String generateFromComponents(TemplateDataFile.ComponentConfig components) {
        StringBuilder content = new StringBuilder();

        // Alert Banner (if configured)
        if (components.getAlertBanner() != null && Boolean.TRUE.equals(components.getAlertBanner().getShow())) {
            content.append(createAlertBanner(components.getAlertBanner()));
        }

        // Greeting
        if (components.getGreeting() != null && !components.getGreeting().isEmpty()) {
            content.append("<p style=\"margin:0 0 20px 0;\">").append(components.getGreeting()).append("</p>");
        }

        // Main Message
        if (components.getMainMessage() != null && !components.getMainMessage().isEmpty()) {
            content.append("<p style=\"margin:0 0 20px 0;\">").append(components.getMainMessage()).append("</p>");
        }

        // Info Box (if configured)
        if (components.getInfoBox() != null && Boolean.TRUE.equals(components.getInfoBox().getShow())) {
            content.append(createInfoBoxFromConfig(components.getInfoBox()));
        }

        // Additional Message
        if (components.getAdditionalMessage() != null && !components.getAdditionalMessage().isEmpty()) {
            content.append("<p style=\"margin:20px 0;\">").append(components.getAdditionalMessage()).append("</p>");
        }

        // Action Button (if configured)
        if (components.getActionButton() != null && Boolean.TRUE.equals(components.getActionButton().getShow())) {
            content.append(htmlRenderer.createButton(
                    components.getActionButton().getText(),
                    components.getActionButton().getUrl()
            ));
        }

        // Footer message in styled template
        String footerMessage = components.getFooterMessage() != null
                ? components.getFooterMessage()
                : "Thank you for using our services.";

        String title = components.getTitle() != null ? components.getTitle() : "Notification";

        return htmlRenderer.createStyledEmailTemplate(title, content.toString(), footerMessage);
    }

    /**
     * Creates an alert banner based on configuration.
     */
    private String createAlertBanner(TemplateDataFile.AlertBanner banner) {
        String backgroundColor;
        String borderColor;
        String textColor;

        switch (banner.getLevel() != null ? banner.getLevel().toLowerCase() : "info") {
            case "warning":
                backgroundColor = "#fff3cd";
                borderColor = "#ffc107";
                textColor = "#856404";
                break;
            case "critical":
                backgroundColor = "#f8d7da";
                borderColor = "#dc3545";
                textColor = "#721c24";
                break;
            case "info":
            default:
                backgroundColor = "#d1ecf1";
                borderColor = "#17a2b8";
                textColor = "#0c5460";
                break;
        }

        String icon = banner.getIcon() != null ? banner.getIcon() + " " : "";

        return "<div style=\"background-color:" + backgroundColor + ";border-left:4px solid " + borderColor +
               ";padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
               "<p style=\"margin:0;font-weight:600;color:" + textColor + ";font-size:16px;\">" +
               icon + banner.getMessage() + "</p>" +
               "</div>";
    }

    /**
     * Creates an info box based on configuration.
     */
    private String createInfoBoxFromConfig(TemplateDataFile.InfoBox infoBox) {
        if (infoBox.getFields() == null || infoBox.getFields().isEmpty()) {
            return "";
        }

        StringBuilder fields = new StringBuilder();
        int fieldCount = infoBox.getFields().size();

        for (int i = 0; i < fieldCount; i++) {
            TemplateDataFile.InfoField field = infoBox.getFields().get(i);
            boolean isLast = (i == fieldCount - 1);
            String marginStyle = isLast ? "margin:0;" : "margin:0 0 10px 0;";

            if (Boolean.TRUE.equals(field.getIsStatusBadge())) {
                // Status badge field
                String badgeColor = getBadgeColor(field.getBadgeColor());
                fields.append("<p style=\"").append(marginStyle).append("\">")
                      .append("<strong style=\"color:#667eea;\">").append(field.getLabel()).append(":</strong> ")
                      .append("<span style=\"display:inline-block;padding:4px 8px;background-color:").append(badgeColor)
                      .append(";color:#ffffff;border-radius:4px;font-size:12px;font-weight:600;\">")
                      .append(field.getValue()).append("</span>")
                      .append("</p>");
            } else {
                // Regular field
                fields.append("<p style=\"").append(marginStyle).append("\">")
                      .append("<strong style=\"color:#667eea;\">").append(field.getLabel()).append(":</strong> ")
                      .append(field.getValue())
                      .append("</p>");
            }
        }

        return htmlRenderer.createInfoBox(fields.toString());
    }

    /**
     * Gets badge color based on color name.
     */
    private String getBadgeColor(String colorName) {
        if (colorName == null) {
            return "#28a745"; // default success green
        }
        switch (colorName.toLowerCase()) {
            case "success":
                return "#28a745";
            case "warning":
                return "#ffc107";
            case "danger":
            case "critical":
                return "#dc3545";
            case "info":
                return "#17a2b8";
            default:
                return "#28a745";
        }
    }

    /**
     * Generates email HTML content based on event type and recipient type.
     * This is the FALLBACK method (Priority 3) for backward compatibility.
     *
     * @param eventType Event type (e.g., GRIEVANCE_RAISED)
     * @param recipientType Recipient type (e.g., DATA_PRINCIPAL)
     * @return Styled HTML email content
     */
    public String generateEmailContent(String eventType, String recipientType) {
        String methodKey = eventType + "_" + recipientType;

        switch (methodKey) {
            // Consent Events - DATA_PRINCIPAL
            case "CONSENT_REQUEST_PENDING_DATA_PRINCIPAL":
                return createConsentRequestPendingDataPrincipalEmail();
            case "CONSENT_RENEWAL_REQUEST_DATA_PRINCIPAL":
                return createConsentRenewalRequestDataPrincipalEmail();
            case "CONSENT_CREATED_DATA_PRINCIPAL":
                return createConsentCreatedDataPrincipalEmail();
            case "CONSENT_UPDATED_DATA_PRINCIPAL":
                return createConsentUpdatedDataPrincipalEmail();
            case "CONSENT_WITHDRAWN_DATA_PRINCIPAL":
                return createConsentWithdrawnDataPrincipalEmail();
            case "CONSENT_EXPIRED_DATA_PRINCIPAL":
                return createConsentExpiredDataPrincipalEmail();
            case "CONSENT_RENEWED_DATA_PRINCIPAL":
                return createConsentRenewedDataPrincipalEmail();

            // Grievance Events - DATA_PRINCIPAL
            case "GRIEVANCE_RAISED_DATA_PRINCIPAL":
                return createGrievanceRaisedDataPrincipalEmail();
            case "GRIEVANCE_INPROCESS_DATA_PRINCIPAL":
                return createGrievanceInprocessDataPrincipalEmail();
            case "GRIEVANCE_L1_ESCALATED_DATA_PRINCIPAL":
                return createGrievanceL1EscalatedDataPrincipalEmail();
            case "GRIEVANCE_L2_ESCALATED_DATA_PRINCIPAL":
                return createGrievanceL2EscalatedDataPrincipalEmail();
            case "GRIEVANCE_RESOLVED_DATA_PRINCIPAL":
                return createGrievanceResolvedDataPrincipalEmail();
            case "GRIEVANCE_DENIED_DATA_PRINCIPAL":
                return createGrievanceDeniedDataPrincipalEmail();
            case "GRIEVANCE_CLOSED_DATA_PRINCIPAL":
                return createGrievanceClosedDataPrincipalEmail();
            case "GRIEVANCE_STATUS_UPDATED_DATA_PRINCIPAL":
                return createGrievanceStatusUpdatedDataPrincipalEmail();

            // Grievance Events - DATA_PROTECTION_OFFICER
            case "GRIEVANCE_RAISED_DATA_PROTECTION_OFFICER":
                return createGrievanceRaisedDpoEmail();
            case "GRIEVANCE_INPROCESS_DATA_PROTECTION_OFFICER":
                return createGrievanceInprocessDpoEmail();
            case "GRIEVANCE_L1_ESCALATED_DATA_PROTECTION_OFFICER":
                return createGrievanceL1EscalatedDpoEmail();
            case "GRIEVANCE_L2_ESCALATED_DATA_PROTECTION_OFFICER":
                return createGrievanceL2EscalatedDpoEmail();
            case "GRIEVANCE_RESOLVED_DATA_PROTECTION_OFFICER":
                return createGrievanceResolvedDpoEmail();
            case "GRIEVANCE_DENIED_DATA_PROTECTION_OFFICER":
                return createGrievanceDeniedDpoEmail();
            case "GRIEVANCE_CLOSED_DATA_PROTECTION_OFFICER":
                return createGrievanceClosedDpoEmail();
            case "GRIEVANCE_STATUS_UPDATED_DATA_PROTECTION_OFFICER":
                return createGrievanceStatusUpdatedDpoEmail();

            // Data Events - DATA_PRINCIPAL
            case "DATA_DELETED_DATA_PRINCIPAL":
                return createDataDeletedDataPrincipalEmail();
            case "DATA_SHARED_DATA_PRINCIPAL":
                return createDataSharedDataPrincipalEmail();

            default:
                log.warn("No email content generator found for: {}", methodKey);
                return createDefaultEmail();
        }
    }

    // ==================== CONSENT EVENTS - DATA_PRINCIPAL ====================

    private String createConsentRequestPendingDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your consent request is currently pending approval with our team.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Requested On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Purpose:</strong> <#ARG5></p>"
                ) +
                "<p style=\"margin:20px 0;\">We will review your consent request and notify you once a decision is made.</p>";

        return htmlRenderer.createStyledEmailTemplate("Consent Request Pending", content,
                "Thank you for your patience. We will process your request as soon as possible.");
    }

    private String createConsentRenewalRequestDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your consent is approaching its expiration date and requires renewal.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Expires On:</strong> <#ARG4></p>"
                ) +
                "<p style=\"margin:20px 0;\">Please review and renew your consent to continue receiving services.</p>" +
                htmlRenderer.createButton("Renew Consent", "<#ARG5>");

        return htmlRenderer.createStyledEmailTemplate("Consent Renewal Required", content,
                "Thank you for keeping your consent up to date.");
    }

    private String createConsentCreatedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your consent has been successfully created and is now active.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Created On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Purpose:</strong> <#ARG5></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Expires On:</strong> <#ARG6></p>"
                ) +
                "<p style=\"margin:20px 0;\">This consent allows us to process your data in accordance with your preferences.</p>";

        return htmlRenderer.createStyledEmailTemplate("Consent Successfully Created", content,
                "You can modify or withdraw your consent at any time through your account settings.");
    }

    private String createConsentUpdatedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your consent has been successfully updated with the latest changes.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Updated On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Details:</strong> <#ARG5></p>"
                ) +
                "<p style=\"margin:20px 0;\">The updated consent is now active and will be applied to all future data processing activities.</p>";

        return htmlRenderer.createStyledEmailTemplate("Consent Updated", content,
                "Thank you for keeping your consent preferences up to date.");
    }

    private String createConsentWithdrawnDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your consent has been successfully withdrawn as per your request.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Withdrawn On:</strong> <#ARG4></p>"
                ) +
                "<p style=\"margin:20px 0;\">We will cease all data processing activities covered under this consent immediately.</p>";

        return htmlRenderer.createStyledEmailTemplate("Consent Withdrawn", content,
                "Your data protection rights are important to us.");
    }

    private String createConsentExpiredDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your consent has expired and is no longer active.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Expired On:</strong> <#ARG4></p>"
                ) +
                "<p style=\"margin:20px 0;\">To continue receiving services, please renew your consent.</p>" +
                htmlRenderer.createButton("Renew Consent", "<#ARG5>");

        return htmlRenderer.createStyledEmailTemplate("Consent Expired", content,
                "Renew your consent to continue enjoying our services.");
    }

    private String createConsentRenewedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your consent has been successfully renewed.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Consent ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Renewed On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">New Expiry Date:</strong> <#ARG5></p>"
                ) +
                "<p style=\"margin:20px 0;\">Your consent is now active for another term.</p>";

        return htmlRenderer.createStyledEmailTemplate("Consent Renewed", content,
                "Thank you for renewing your consent.");
    }

    // ==================== GRIEVANCE EVENTS - DATA_PRINCIPAL ====================

    private String createGrievanceRaisedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your grievance has been successfully raised and is now being processed by our team.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Raised On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Current Status:</strong> <span style=\"display:inline-block;padding:4px 8px;background-color:#28a745;color:#ffffff;border-radius:4px;font-size:12px;font-weight:600;\"><#ARG5></span></p>"
                ) +
                "<p style=\"margin:20px 0;\">Our team will review your grievance and respond within 3-5 business days. You can track the progress of your grievance anytime using the link below.</p>" +
                htmlRenderer.createButton("Track Grievance", "<#ARG6>") +
                "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">If you have any questions, please don't hesitate to contact our support team.</p>";

        return htmlRenderer.createStyledEmailTemplate("Grievance Successfully Raised", content,
                "Thank you for bringing this to our attention. We are committed to resolving your grievance.");
    }

    private String createGrievanceInprocessDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your grievance is now being actively processed by our team.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Updated On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Current Status:</strong> <span style=\"display:inline-block;padding:4px 8px;background-color:#ffc107;color:#000000;border-radius:4px;font-size:12px;font-weight:600;\"><#ARG5></span></p>"
                ) +
                "<p style=\"margin:20px 0;\">We are working diligently to address your concerns. You will be notified of any updates.</p>" +
                htmlRenderer.createButton("Track Grievance", "<#ARG6>");

        return htmlRenderer.createStyledEmailTemplate("Grievance In Process", content,
                "We appreciate your patience as we work to resolve your grievance.");
    }

    private String createGrievanceL1EscalatedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your grievance has been escalated to <strong>Level 1</strong> for priority review.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Escalated On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Current Status:</strong> <span style=\"display:inline-block;padding:4px 8px;background-color:#ff6b6b;color:#ffffff;border-radius:4px;font-size:12px;font-weight:600;\"><#ARG5></span></p>"
                ) +
                "<p style=\"margin:20px 0;\">A senior team member will now review your grievance and work towards a swift resolution.</p>" +
                htmlRenderer.createButton("Track Grievance", "<#ARG6>");

        return htmlRenderer.createStyledEmailTemplate("Grievance Escalated to Level 1", content,
                "We take your concerns seriously and are committed to finding a resolution.");
    }

    private String createGrievanceL2EscalatedDataPrincipalEmail() {
        String content = "<div style=\"background-color:#fff3cd;border-left:4px solid #dc3545;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                "<p style=\"margin:0;font-weight:600;color:#721c24;font-size:16px;\">⚠️ CRITICAL ESCALATION - Level 2</p>" +
                "</div>" +
                "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your grievance has been escalated to <strong>Level 2</strong> and is now being handled as a critical priority by our senior management team.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Escalated On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Current Status:</strong> <span style=\"display:inline-block;padding:4px 8px;background-color:#dc3545;color:#ffffff;border-radius:4px;font-size:12px;font-weight:600;\"><#ARG5></span></p>"
                ) +
                "<p style=\"margin:20px 0;\">This is the highest level of escalation. Our leadership team will provide immediate attention to resolve your grievance as quickly as possible.</p>" +
                htmlRenderer.createButton("Track Grievance", "<#ARG6>") +
                "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">We apologize for any inconvenience and appreciate your patience.</p>";

        return htmlRenderer.createStyledEmailTemplate("CRITICAL: Grievance Escalated to Level 2", content,
                "Your grievance has our highest priority and will be resolved urgently.");
    }

    private String createGrievanceResolvedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">We are pleased to inform you that your grievance has been successfully resolved.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Resolved On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Resolution Details:</strong> <#ARG5></p>"
                ) +
                "<p style=\"margin:20px 0;\">If you have any further questions or concerns, please don't hesitate to reach out.</p>";

        return htmlRenderer.createStyledEmailTemplate("Grievance Resolved", content,
                "Thank you for your patience. We hope the resolution meets your expectations.");
    }

    private String createGrievanceDeniedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">After careful review, we regret to inform you that your grievance has been denied.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Denied On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Reason:</strong> <#ARG5></p>"
                ) +
                "<p style=\"margin:20px 0;\">If you disagree with this decision, you may escalate this matter through appropriate channels.</p>";

        return htmlRenderer.createStyledEmailTemplate("Grievance Denied", content,
                "We understand this may be disappointing. Please contact us if you have any questions.");
    }

    private String createGrievanceClosedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your grievance has been closed.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Closed On:</strong> <#ARG4></p>"
                ) +
                "<p style=\"margin:20px 0;\">Thank you for working with us to address your concerns.</p>";

        return htmlRenderer.createStyledEmailTemplate("Grievance Closed", content,
                "If you have any new concerns in the future, please don't hesitate to reach out.");
    }

    private String createGrievanceStatusUpdatedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">The status of your grievance has been updated.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Updated On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">New Status:</strong> <#ARG5></p>"
                ) +
                "<p style=\"margin:20px 0;\">You can track the latest progress of your grievance using the link below.</p>" +
                htmlRenderer.createButton("Track Grievance", "<#ARG6>");

        return htmlRenderer.createStyledEmailTemplate("Grievance Status Updated", content,
                "We will continue to keep you informed as your grievance progresses.");
    }

    // ==================== GRIEVANCE EVENTS - DATA_PROTECTION_OFFICER ====================

    private String createGrievanceRaisedDpoEmail() {
        String content = "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert</p>" +
                "</div>" +
                "<p style=\"margin:0 0 20px 0;\">A grievance event has occurred that requires your attention and oversight.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                ) +
                "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                "</div>" +
                "<p style=\"margin:20px 0;\">Please review this grievance and take appropriate action in accordance with data protection regulations.</p>" +
                "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>";

        return htmlRenderer.createStyledEmailTemplate("DPO Alert: Grievance Event", content,
                "Your oversight ensures compliance with data protection requirements.");
    }

    private String createGrievanceInprocessDpoEmail() {
        return createGenericGrievanceDpoEmail("Grievance In Process");
    }

    private String createGrievanceL1EscalatedDpoEmail() {
        String content = "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert - Level 1 Escalation</p>" +
                "</div>" +
                "<p style=\"margin:0 0 20px 0;\">A grievance has been escalated to <strong>Level 1</strong> and requires your attention and oversight.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                ) +
                "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                "</div>" +
                "<p style=\"margin:20px 0;\">This grievance has been escalated and requires priority review in accordance with data protection regulations.</p>" +
                "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>";

        return htmlRenderer.createStyledEmailTemplate("DPO Alert: L1 Escalation", content,
                "Priority attention required for Level 1 grievance escalation.");
    }

    private String createGrievanceL2EscalatedDpoEmail() {
        String content = "<div style=\"background-color:#fff3cd;border-left:4px solid #dc3545;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                "<p style=\"margin:0;font-weight:600;color:#721c24;font-size:16px;\">🚨 CRITICAL DPO ALERT - Level 2 Escalation</p>" +
                "</div>" +
                "<p style=\"margin:0 0 20px 0;\">A grievance has been escalated to <strong>Level 2</strong> and requires your immediate attention and oversight.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                ) +
                "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                "</div>" +
                "<p style=\"margin:20px 0;\">This is the highest level of escalation and requires urgent review and action in accordance with data protection regulations.</p>" +
                "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a critical system-generated alert from the DPDP Notification System.</p>";

        return htmlRenderer.createStyledEmailTemplate("CRITICAL DPO ALERT: L2 Escalation", content,
                "Immediate attention required for Level 2 grievance escalation.");
    }

    private String createGrievanceResolvedDpoEmail() {
        return createGenericGrievanceDpoEmail("Grievance Resolved");
    }

    private String createGrievanceDeniedDpoEmail() {
        return createGenericGrievanceDpoEmail("Grievance Denied");
    }

    private String createGrievanceClosedDpoEmail() {
        return createGenericGrievanceDpoEmail("Grievance Closed");
    }

    private String createGrievanceStatusUpdatedDpoEmail() {
        return createGenericGrievanceDpoEmail("Grievance Status Updated");
    }

    private String createGenericGrievanceDpoEmail(String title) {
        String content = "<div style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:0 0 20px 0;border-radius:4px;\">" +
                "<p style=\"margin:0;font-weight:600;color:#856404;font-size:16px;\">⚠️ Data Protection Officer Alert</p>" +
                "</div>" +
                "<p style=\"margin:0 0 20px 0;\">A grievance event has occurred that requires your attention and oversight.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Business ID:</strong> <#ARG1></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Grievance ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Event Type:</strong> <#ARG3></p>"
                ) +
                "<div style=\"background-color:#f8f9fa;padding:15px;margin:20px 0;border-radius:4px;border:1px solid #dee2e6;\">" +
                "<p style=\"margin:0 0 10px 0;font-weight:600;color:#495057;\">JWT Authentication Token:</p>" +
                "<pre style=\"background-color:#ffffff;padding:12px;border-radius:4px;overflow-x:auto;word-wrap:break-word;border:1px solid #ced4da;margin:0;font-size:11px;line-height:1.4;\"><#ARG4></pre>" +
                "</div>" +
                "<p style=\"margin:20px 0;\">Please review this grievance and take appropriate action in accordance with data protection regulations.</p>" +
                "<p style=\"margin:20px 0 0 0;font-size:13px;color:#6c757d;\">This is a system-generated alert from the DPDP Notification System.</p>";

        return htmlRenderer.createStyledEmailTemplate("DPO Alert: " + title, content,
                "Your oversight ensures compliance with data protection requirements.");
    }

    // ==================== DATA EVENTS - DATA_PRINCIPAL ====================

    private String createDataDeletedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your data has been successfully deleted from our systems as requested.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Data Reference ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Organization:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Deleted On:</strong> <#ARG4></p>"
                ) +
                "<p style=\"margin:20px 0;\">All data associated with this reference has been permanently removed from our systems.</p>";

        return htmlRenderer.createStyledEmailTemplate("Data Deletion Confirmation", content,
                "Your privacy is important to us. Thank you for trusting us with your data.");
    }

    private String createDataSharedDataPrincipalEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">Dear <strong><#ARG1></strong>,</p>" +
                "<p style=\"margin:0 0 20px 0;\">Your data has been shared as per your consent.</p>" +
                htmlRenderer.createInfoBox(
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Data Reference ID:</strong> <#ARG2></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Shared With:</strong> <#ARG3></p>" +
                        "<p style=\"margin:0 0 10px 0;\"><strong style=\"color:#667eea;\">Shared On:</strong> <#ARG4></p>" +
                        "<p style=\"margin:0;\"><strong style=\"color:#667eea;\">Purpose:</strong> <#ARG5></p>"
                ) +
                "<p style=\"margin:20px 0;\">This data sharing activity was conducted in accordance with your consent and our data protection policies.</p>";

        return htmlRenderer.createStyledEmailTemplate("Data Sharing Notification", content,
                "You can manage your data sharing preferences anytime through your account settings.");
    }

    // ==================== DEFAULT ====================

    private String createDefaultEmail() {
        String content = "<p style=\"margin:0 0 20px 0;\">This is an automated notification from the DPDP Notification System.</p>" +
                "<p style=\"margin:20px 0;\">For more information, please contact support.</p>";

        return htmlRenderer.createStyledEmailTemplate("Notification", content,
                "This is an automated message from the DPDP Notification System.");
    }
}
