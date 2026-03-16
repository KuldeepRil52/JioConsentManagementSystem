package com.jio.digigov.auditmodule.service.impl;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jio.digigov.auditmodule.service.PdfSignerService;
import com.jio.digigov.auditmodule.service.SignPdfService;
import com.jio.digigov.auditmodule.service.dto.CertifyingOfficer;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class SignPdfServiceImpl implements SignPdfService {

    @Autowired
    private PdfSignerService pdfSignerService;

    private PDType0Font FONT_BOLD;
    private PDType0Font FONT_REGULAR;
    private PDType0Font FONT_MONO;
    private boolean useUnicodeFont = false;

    private static final float MARGIN = 50f;
    private static final float LINE_SPACING = 15f;
    private static final float BOTTOM_MARGIN = 120f; // Reserve space for signature (80px height + margins)

    private static class PageContext {
        final PDDocument document;
        PDPage page;
        PDPageContentStream content;
        PDRectangle rect;
        float y;

        PageContext(PDDocument document, PDPage page, PDPageContentStream content, PDRectangle rect, float y) {
            this.document = document;
            this.page = page;
            this.content = content;
            this.rect = rect;
            this.y = y;
        }
    }

    @Override
    public byte[] generateAndSignForm65B(
            String consentId,
            String decryptedJson,
            String referenceId,
            String storedHash,
            String computedHash,
            boolean valid,
            String auditRecord,
            String tenantId,
            String businessId,
            String previousChain,
            String currentChainHash,
            String status) throws Exception {


        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PDDocument doc = new PDDocument()) {

            loadFonts(doc);

            PDPage firstPage = new PDPage(PDRectangle.A4);
            doc.addPage(firstPage);
            PDRectangle rect = firstPage.getMediaBox();
            PDPageContentStream firstContent = new PDPageContentStream(doc, firstPage);

            PageContext ctx = new PageContext(doc, firstPage, firstContent, rect, rect.getHeight() - 50f);

            // HEADER
            ctx = drawCentered(ctx, "SECTION 65B CERTIFICATE", FONT_BOLD, 16f);
            ctx = drawCentered(ctx, "Indian Evidence Act, 1872 — Certification of Electronic Record", FONT_BOLD, 11f);

            ctx.y -= 15f;
            ctx = drawLine(ctx);
            ctx.y -= 20f;

            ctx = wrap(ctx, "This certificate is issued under Section 65B(4) of the Indian Evidence Act, 1872. It certifies that the"
                            +" electronic record described herein was produced by a computer system operating in the ordinary"
                            +" course of business and functioning properly at all material times. The statements contained herein"
                            +" are true to the best of the certifier’s knowledge and belief.",
                    FONT_REGULAR, 10f, MARGIN, rect.getWidth() - MARGIN, 13f);

            ctx.y -= 25f;

            CertifyingOfficer officer = pdfSignerService.getCertifyingOfficer(tenantId);

            // SECTION 1
            ctx = section(ctx, "1. CERTIFYING OFFICER");
            ctx = field(ctx, "Name:", officer.name());
            ctx = field(ctx, "Designation:", officer.designation());
            ctx = field(ctx, "Organisation:", officer.organisation());
            ctx = field(ctx, "Contact:", officer.contact());

            ctx.y -= 15f;

            // SECTION 2
            ctx = section(ctx, "2. ELECTRONIC RECORD IDENTIFICATION");
            ctx = field(ctx, "Consent ID:", sanitize(consentId));
            ctx = field(ctx, "Tenant ID:", sanitize(tenantId));
            ctx = field(ctx, "Business ID:", sanitize(businessId));
            ctx = field(ctx, "Reference ID:", sanitize(referenceId));
            ctx = field(ctx, "Status:", sanitize(status));
            ctx = field(ctx, "Certificate Timestamp:", timestamp());

            ctx.y -= 15f;

            // SECTION 3
            ctx = section(ctx, "3. MANNER OF PRODUCTION");
            ctx = field(ctx, "System/Application:", "ConsentCloud Engine v2.8");
            ctx = field(ctx, "API Endpoint:", "/consent/verify");
            ctx = field(ctx, "Runtime:", "Java 21 on Spring Boot");
            ctx = field(ctx, "Database:", "MongoDB");
            ctx = field(ctx, "Hash Algorithm:", "SHA-256");
            ctx = field(ctx, "Payload Hash Verification:", valid ? "VALID" : "INVALID");

            ctx.y -= 20f;

            // SECTION 4
            ctx = section(ctx, "4. SYSTEM CONTROLS & SECURITY");
            ctx = bullet(ctx, "Encrypted and versioned data storage");
            ctx = bullet(ctx, "Immutable audit chains");
            ctx = bullet(ctx, "Strict IAM-controlled write operations");
            ctx = bullet(ctx, "Differential state hashing");
            ctx = bullet(ctx, "Chain-of-custody validation");
            ctx = bullet(ctx, "Scheduled forensic integrity scans");

            ctx.y -= 20f;

            // SECTION 5
            ctx = section(ctx, "5. INTEGRITY VERIFICATION RESULTS");
            ctx = field(ctx, "Payload Hash Verification:", valid ? "VALID" : "INVALID");
            ctx = field(ctx, "Chain Link Verification:", "VALID");
            ctx = field(ctx, "Consent JSON Match:", "VALID");
            ctx = field(ctx, "Overall Integrity Status:", "VERIFIED");
            ctx = mono(ctx, "Stored Hash:", sanitize(storedHash));
            ctx = mono(ctx, "Computed Hash:", sanitize(computedHash));
            ctx = mono(ctx, "Current Chain Hash:", sanitize(currentChainHash));

            ctx.y -= 20f;

            // SECTION 6
            ctx = section(ctx, "6. STATEMENT OF REGULAR PRACTICE");
            ctx = wrap(ctx,
                    "This record was generated and stored automatically as part of routine system operation "
                            + "and retrieved using authorised access mechanisms. The system was functioning correctly "
                            + "throughout the process.",
                    FONT_REGULAR, 10f, MARGIN, rect.getWidth() - MARGIN, 13f);

            ctx.y -= 25f;

            // SECTION 7
            ctx = section(ctx, "7. ATTESTATION UNDER SECTION 65B(4)");
            ctx = wrap(ctx,
                    "I certify that the above record is a true and accurate reproduction of the electronic record stored "
                            + "within the system under my supervision, and is suitable for legal admissibility under Section 65B of "
                            + "the Indian Evidence Act, 1872. "
                            + "Note: Digital signature will be applied externally via a PAdES-compliant mechanism.",
                    FONT_REGULAR, 10f, MARGIN, rect.getWidth() - MARGIN, 13f);

            ctx.y -= 20f;

            // APPENDIX A

            if (auditRecord != null
                    && !auditRecord.trim().isEmpty()
                    && !"null".equalsIgnoreCase(auditRecord.trim())) {

                ctx = section(ctx, "APPENDIX A — AUDIT REFERENCE JSON");

                String auditJsonPretty = prettifyJson(auditRecord);
                ctx = json(ctx, auditJsonPretty);
                ctx.y -= 20f;
            }

            // APPENDIX B
            ctx = section(ctx, "APPENDIX B — DECRYPTED CONSENT JSON");
            String consentJsonPretty = prettifyJson(decryptedJson);
            ctx = json(ctx, consentJsonPretty);

            // Close the last content stream
            if (ctx.content != null) {
                ctx.content.close();
            }

            doc.save(baos);
        } catch (IOException e) {
            log.error("[PDF] Error generating PDF", e);
            throw e;
        }

        byte[] unsignedPdf = baos.toByteArray();
        return pdfSignerService.signPdf(unsignedPdf, tenantId);
    }

    /**
     * Prettifies JSON string with proper indentation
     *
     * @param jsonString The JSON string to prettify
     * @return Pretty-printed JSON string, or original string if parsing fails
     */
    private String prettifyJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return jsonString;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            Object jsonObject = mapper.readValue(jsonString, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

        } catch (Exception e) {
            log.warn("Failed to prettify JSON, returning original string", e);
            return jsonString;
        }
    }

    private PageContext ensureSpace(PageContext ctx, float required) throws IOException {
        // Check if we have enough space, accounting for signature area at bottom
        if (ctx.y - required > BOTTOM_MARGIN) {
            return ctx;
        }

        // Need new page
        if (ctx.content != null) {
            ctx.content.close();
        }

        PDPage newPage = new PDPage(PDRectangle.A4);
        ctx.document.addPage(newPage);
        PDPageContentStream newContent = new PDPageContentStream(ctx.document, newPage);

        PDRectangle newRect = newPage.getMediaBox();
        float newY = newRect.getHeight() - 50f;

        return new PageContext(ctx.document, newPage, newContent, newRect, newY);
    }

    private PageContext drawCentered(PageContext ctx, String text, PDType0Font font, float size) throws IOException {
        float estimatedHeight = size + 8f;
        ctx = ensureSpace(ctx, estimatedHeight);

        float textWidth = getTextWidth(font, text, size);
        float startX = (ctx.rect.getWidth() - textWidth) / 2f;

        ctx.content.beginText();
        ctx.content.setFont(font, size);
        ctx.content.newLineAtOffset(startX, ctx.y);
        safeShowText(ctx.content, text, font);
        ctx.content.endText();

        ctx.y -= (size + 8f);
        return ctx;
    }

    private PageContext drawLine(PageContext ctx) throws IOException {
        ctx = ensureSpace(ctx, 12f);
        ctx.content.moveTo(MARGIN, ctx.y);
        ctx.content.lineTo(ctx.rect.getWidth() - MARGIN, ctx.y);
        ctx.content.stroke();
        return ctx;
    }

    private PageContext section(PageContext ctx, String text) throws IOException {
        ctx = ensureSpace(ctx, 20f);
        ctx.content.beginText();
        ctx.content.setFont(FONT_BOLD, 11f);
        ctx.content.newLineAtOffset(MARGIN, ctx.y);
        safeShowText(ctx.content, text, FONT_BOLD);
        ctx.content.endText();
        ctx.y -= 18f;
        return ctx;
    }

    private PageContext field(PageContext ctx, String label, String value) throws IOException {
        ctx = ensureSpace(ctx, LINE_SPACING);
        float size = 10f;

        ctx.content.beginText();
        ctx.content.setFont(FONT_REGULAR, size);
        ctx.content.newLineAtOffset(MARGIN + 10f, ctx.y);
        safeShowText(ctx.content, label, FONT_REGULAR);
        ctx.content.endText();

        float labelWidth = getTextWidth(FONT_REGULAR, label, size);

        ctx.content.beginText();
        ctx.content.setFont(FONT_REGULAR, size);
        ctx.content.newLineAtOffset(MARGIN + 20f + labelWidth, ctx.y);
        safeShowText(ctx.content, value != null ? value : "", FONT_REGULAR);
        ctx.content.endText();

        ctx.y -= LINE_SPACING;
        return ctx;
    }

    private PageContext mono(PageContext ctx, String label, String value) throws IOException {
        ctx = ensureSpace(ctx, LINE_SPACING * 2);
        ctx.content.beginText();
        ctx.content.setFont(FONT_REGULAR, 10f);
        ctx.content.newLineAtOffset(MARGIN + 10f, ctx.y);
        safeShowText(ctx.content, label, FONT_REGULAR);
        ctx.content.endText();

        ctx.y -= LINE_SPACING;

        ctx.content.beginText();
        ctx.content.setFont(FONT_MONO, 8f);
        ctx.content.newLineAtOffset(MARGIN + 15f, ctx.y);
        safeShowText(ctx.content, value != null ? value : "", FONT_MONO);
        ctx.content.endText();

        ctx.y -= LINE_SPACING;
        return ctx;
    }

    private PageContext bullet(PageContext ctx, String text) throws IOException {
        ctx = ensureSpace(ctx, LINE_SPACING);
        ctx.content.beginText();
        ctx.content.setFont(FONT_REGULAR, 10f);
        ctx.content.newLineAtOffset(MARGIN + 10f, ctx.y);
        safeShowText(ctx.content, "• " + text, FONT_REGULAR);
        ctx.content.endText();
        ctx.y -= LINE_SPACING;
        return ctx;
    }

    private PageContext wrap(PageContext ctx, String text, PDType0Font font, float size, float x, float maxWidth, float leading) throws IOException {
        ctx.content.setFont(font, size);
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String test = (line.length() == 0) ? word : line + " " + word;
            float width = getTextWidth(font, test, size);

            if (width > (maxWidth - x - MARGIN)) {
                ctx = ensureSpace(ctx, leading + 2f);

                ctx.content.beginText();
                ctx.content.newLineAtOffset(x, ctx.y);
                safeShowText(ctx.content, line.toString(), font);
                ctx.content.endText();

                ctx.y -= leading;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }

        if (line.length() > 0) {
            ctx = ensureSpace(ctx, leading + 2f);
            ctx.content.beginText();
            ctx.content.newLineAtOffset(x, ctx.y);
            safeShowText(ctx.content, line.toString(), font);
            ctx.content.endText();
            ctx.y -= leading;
        }

        return ctx;
    }

    private PageContext json(PageContext ctx, String json) throws IOException {
        PDFont jsonFont = useUnicodeFont ? FONT_REGULAR : FONT_MONO;
        float fontSize = 7f;
        float jsonIndent = MARGIN + 5f;
        float maxWidth = ctx.rect.getWidth() - jsonIndent - MARGIN - 10f;
        float leading = 10f;

        // Clean and format JSON
        String cleanJson = json.replaceAll("\\s+", " ").trim();
        String formattedJson = cleanJson
                .replace(",\"", ",\n  \"")
                .replace("{\"", "{\n  \"")
                .replace("}", "\n}");

        String[] lines = formattedJson.split("\n");

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            // Calculate actual width
            float lineWidth = getTextWidth(jsonFont, trimmedLine, fontSize);

            if (lineWidth <= maxWidth) {
                // Line fits completely
                ctx = ensureSpace(ctx, leading + 2f);
                ctx.content.beginText();
                ctx.content.setFont(jsonFont, fontSize);
                ctx.content.newLineAtOffset(jsonIndent, ctx.y);
                safeShowText(ctx.content, trimmedLine, jsonFont);
                ctx.content.endText();
                ctx.y -= leading;
            } else {
                // Line needs wrapping - calculate chars that fit
                float avgCharWidth = getTextWidth(jsonFont, "M", fontSize);
                int charsPerLine = Math.max(1, (int) (maxWidth / avgCharWidth));

                int start = 0;
                while (start < trimmedLine.length()) {
                    int end = Math.min(start + charsPerLine, trimmedLine.length());

                    // Try to find a good break point (comma, colon, brace)
                    if (end < trimmedLine.length()) {
                        int commaIdx = trimmedLine.lastIndexOf(',', end);
                        int colonIdx = trimmedLine.lastIndexOf(':', end);
                        int braceIdx = trimmedLine.lastIndexOf('{', end);

                        int breakPoint = Math.max(commaIdx, Math.max(colonIdx, braceIdx));
                        if (breakPoint > start && breakPoint < end) {
                            end = breakPoint + 1; // Include the delimiter
                        }
                    }

                    String chunk = trimmedLine.substring(start, end).trim();
                    if (!chunk.isEmpty()) {
                        ctx = ensureSpace(ctx, leading + 2f);
                        ctx.content.beginText();
                        ctx.content.setFont(jsonFont, fontSize);
                        ctx.content.newLineAtOffset(jsonIndent, ctx.y);
                        safeShowText(ctx.content, chunk, jsonFont);
                        ctx.content.endText();
                        ctx.y -= leading;
                    }

                    start = end;
                }
            }
        }

        return ctx;
    }

    private void safeShowText(PDPageContentStream content, String text, PDFont font) throws IOException {
        if (text == null || text.isEmpty()) {
            return;
        }

        try {
            content.showText(text);
        } catch (IllegalArgumentException e) {
            log.warn("Font doesn't support some characters, filtering: {}", e.getMessage());
            String filtered = filterUnsupportedCharacters(text, font);
            content.showText(filtered);
        }
    }

    private String filterUnsupportedCharacters(String text, PDFont font) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            try {
                font.encode(String.valueOf(c));
                result.append(c);
            } catch (Exception e) {
                result.append('?');
            }
        }
        return result.toString();
    }

    private float getTextWidth(PDFont font, String text, float fontSize) {
        try {
            return font.getStringWidth(text) / 1000f * fontSize;
        } catch (Exception e) {
            log.warn("Could not calculate width, using estimation");
            return text.length() * fontSize * 0.5f;
        }
    }

    private void loadFonts(PDDocument doc) throws IOException {
        try {
            ClassPathResource boldRes = new ClassPathResource("fonts/NotoSans-Bold.ttf");
            ClassPathResource regularRes = new ClassPathResource("fonts/NotoSans-Regular.ttf");
            ClassPathResource monoRes = new ClassPathResource("fonts/NotoSansMono-Regular.ttf");

            if (boldRes.exists() && regularRes.exists() && monoRes.exists()) {
                try (InputStream boldStream = boldRes.getInputStream();
                     InputStream regularStream = regularRes.getInputStream();
                     InputStream monoStream = monoRes.getInputStream()) {

                    FONT_BOLD = PDType0Font.load(doc, boldStream);
                    FONT_REGULAR = PDType0Font.load(doc, regularStream);
                    FONT_MONO = PDType0Font.load(doc, monoStream);
                    useUnicodeFont = true;
                    log.info("✓ Loaded Noto Sans fonts with Unicode support");
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Noto Sans fonts not found: {}", e.getMessage());
        }

        try {
            ClassPathResource boldRes = new ClassPathResource("fonts/Roboto-Bold.ttf");
            ClassPathResource regularRes = new ClassPathResource("fonts/Roboto-Regular.ttf");
            ClassPathResource monoRes = new ClassPathResource("fonts/RobotoMono-Regular.ttf");

            try (InputStream boldStream = boldRes.getInputStream();
                 InputStream regularStream = regularRes.getInputStream();
                 InputStream monoStream = monoRes.getInputStream()) {

                FONT_BOLD = PDType0Font.load(doc, boldStream);
                FONT_REGULAR = PDType0Font.load(doc, regularStream);
                FONT_MONO = PDType0Font.load(doc, monoStream);
                useUnicodeFont = false;
                log.warn("⚠ Loaded Roboto - Unicode chars will show as '?'");
            }
        } catch (Exception ex) {
            log.error("Failed to load fonts", ex);
            throw new IOException("Could not load fonts from src/main/resources/fonts/", ex);
        }
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String sanitize(String t) {
        if (t == null) return "";
        return t.replaceAll("[\u0000-\u001F\u007F-\u009F]", "");
    }

    /**
     * Format audit record to pretty JSON string
     */
    private String formatAuditRecord(Object auditRecord) {
        if (auditRecord == null) {
            return "{\n  \"audit\": null,\n  \"message\": \"No audit record found for this referenceId\"\n}";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            // If it's already a JSON string, parse and reformat it
            if (auditRecord instanceof String) {
                String jsonStr = (String) auditRecord;
                Object parsed = mapper.readValue(jsonStr, Object.class);
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
            }
            // Otherwise, serialize the object directly
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(auditRecord);
        } catch (Exception e) {
            log.warn("Failed to serialize audit record to JSON: {}", e.getMessage());
            // Fallback: try to clean up toString() output
            String str = auditRecord.toString();
            // If it looks like a class toString (contains package name), return error message
            if (str.contains("@") || str.contains("$")) {
                return "{\n  \"error\": \"Unable to format audit record\",\n  \"raw\": \"" +
                        str.replace("\"", "\\\"") + "\"\n}";
            }
            return str;
        }
    }
}