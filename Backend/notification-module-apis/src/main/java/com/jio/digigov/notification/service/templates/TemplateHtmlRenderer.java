package com.jio.digigov.notification.service.templates;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to render HTML email template components.
 *
 * <p>This service provides utility methods to generate styled HTML components
 * for email templates with inline CSS for email client compatibility.</p>
 *
 * @author DPDP Notification Team
 * @since 2.0.0
 */
@Service
@Slf4j
public class TemplateHtmlRenderer {

    /**
     * Creates a styled email template with gradient header.
     */
    public String createStyledEmailTemplate(String title, String content, String footerText) {
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
                "<tr><td style=\"background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:30px;text-align:center;border-radius:8px 8px 0 0;\">" +
                "<h1 style=\"margin:0;color:#ffffff;font-size:24px;font-weight:600;\">" + title + "</h1>" +
                "</td></tr>" +
                "<tr><td style=\"padding:40px 30px;color:#333333;font-size:15px;line-height:1.6;\">" +
                content +
                "</td></tr>" +
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
     * Creates an info box with styled border.
     */
    public String createInfoBox(String content) {
        return "<div style=\"background-color:#e7f3ff;border-left:4px solid#667eea;padding:15px;margin:20px 0;border-radius:4px;\">" +
                content +
                "</div>";
    }

    /**
     * Creates a styled button/CTA.
     */
    public String createButton(String text, String url) {
        return "<div style=\"text-align:center;margin:30px 0;\">" +
                "<a href=\"" + url + "\" style=\"display:inline-block;padding:12px 30px;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:#ffffff;text-decoration:none;border-radius:5px;font-weight:600;font-size:14px;\">" +
                text +
                "</a></div>";
    }
}
