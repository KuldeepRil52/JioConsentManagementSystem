package com.jio.partnerportal.util;

import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.exception.PartnerPortalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class DocumentValidator {

    private static final Logger log = LoggerFactory.getLogger(DocumentValidator.class);

    private static final Map<String, String> EXTENSION_TO_MIME = new HashMap<>();

    static {
        EXTENSION_TO_MIME.put("pdf", "application/pdf");
        EXTENSION_TO_MIME.put("png", "image/png");
        EXTENSION_TO_MIME.put("jpg", "image/jpeg");
        EXTENSION_TO_MIME.put("jpeg", "image/jpeg");
        EXTENSION_TO_MIME.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        EXTENSION_TO_MIME.put("doc", "application/msword");
        EXTENSION_TO_MIME.put("zip", "application/zip");
        EXTENSION_TO_MIME.put("txt", "text/plain");
        // Certificate related
        EXTENSION_TO_MIME.put("cer", "application/pkix-cert");
        EXTENSION_TO_MIME.put("crt", "application/pkix-cert");
        EXTENSION_TO_MIME.put("pem", "application/x-pem-file");
        EXTENSION_TO_MIME.put("der", "application/pkix-cert");
        EXTENSION_TO_MIME.put("p12", "application/x-pkcs12");
        EXTENSION_TO_MIME.put("pfx", "application/x-pkcs12");
    }

    /**
     * Validates a base64-encoded document for basic consistency checks:
     *  - valid base64
     *  - file signature (magic bytes) consistent with file type
     *  - content type consistent with file extension
     *  - size roughly matches decoded length
     *
     * Throws PartnerPortalException with an appropriate error code on failure.
     */
    public void validateDocument(String base64Content, String fileName, String declaredContentType, long declaredSize) throws PartnerPortalException {
        // Empty or null check
        if (base64Content == null || base64Content.isEmpty()) {
            log.warn("Empty document content for file: {}", fileName);
            throw new PartnerPortalException(ErrorCodes.JCMP6009);
        }

        String cleaned = stripDataUriPrefix(base64Content.trim());

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(cleaned.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid base64 for file: {}", fileName);
            throw new PartnerPortalException(ErrorCodes.JCMP6010);
        }

        // size check (allow small variance)
        if (declaredSize > 0) {
            long diff = Math.abs(decoded.length - declaredSize);
            double pct = (double) diff / (double) Math.max(1, declaredSize);
            if (pct > 0.10d && diff > 10) { // more than 10% OR more than 10 bytes
                log.warn("Declared size {} differs significantly from actual size {} for file {}", declaredSize, decoded.length, fileName);
                throw new PartnerPortalException(ErrorCodes.JCMP6013);
            }
        }

        // signature/content-type/extension checks
        boolean matched = matchesKnownSignature(decoded, declaredContentType, fileName);
        if (!matched) {
            String ext = extractExtension(fileName);
            if (ext == null) {
                log.warn("Unsupported file type (no extension) for file: {}", fileName);
                throw new PartnerPortalException(ErrorCodes.JCMP6011);
            }
            // if extension is unknown in our mapping, report unsupported file type
            if (!EXTENSION_TO_MIME.containsKey(ext.toLowerCase(Locale.ROOT))) {
                log.warn("Unsupported file extension '{}' for file: {}", ext, fileName);
                throw new PartnerPortalException(ErrorCodes.JCMP6011);
            }

            // we have a known extension but signature didn't match
            log.warn("File signature/content-type mismatch for file: {} (declared type: {})", fileName, declaredContentType);
            throw new PartnerPortalException(ErrorCodes.JCMP6012);
        }
    }

    private String stripDataUriPrefix(String s) {
        int comma = s.indexOf(",");
        if (s.startsWith("data:") && comma > 0) {
            return s.substring(comma + 1);
        }
        return s;
    }

    private boolean matchesKnownSignature(byte[] data, String declaredContentType, String fileName) {
        if (data == null || data.length == 0) {
            log.debug("matchesKnownSignature: empty data");
            return false;
        }

        // Check pdf
        if (startsWith(data, "%PDF".getBytes(StandardCharsets.US_ASCII))) {
            log.debug("matchesKnownSignature: detected PDF signature");
            return containsMime(declaredContentType, "application/pdf") || hasExtension(fileName, "pdf");
        }

        // PNG
        if (data.length >= 8 && (data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            log.debug("matchesKnownSignature: detected PNG signature");
            return containsMime(declaredContentType, "image/png") || hasExtension(fileName, "png");
        }

        // JPEG (starts FF D8)
        if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            log.debug("matchesKnownSignature: detected JPEG signature");
            return containsMime(declaredContentType, "image/jpeg") || hasExtension(fileName, "jpg") || hasExtension(fileName, "jpeg");
        }

        // ZIP / OOXML (docx, xlsx, pptx) start with PK
        if (data.length >= 4 && data[0] == 'P' && data[1] == 'K' && data[2] == 3 && data[3] == 4) {
            log.debug("matchesKnownSignature: detected ZIP/OOXML signature");
            // could be docx, xlsx, pptx or zip
            return containsMime(declaredContentType, "application/zip")
                    || hasExtension(fileName, "zip")
                    || hasExtension(fileName, "docx");
        }

        // MS-OLE Compound File (older .doc)
        if (data.length >= 8 && (data[0] & 0xFF) == 0xD0 && (data[1] & 0xFF) == 0xCF && (data[2] & 0xFF) == 0x11 && (data[3] & 0xFF) == 0xE0) {
            log.debug("matchesKnownSignature: detected MS-OLE Compound File signature");
            return containsMime(declaredContentType, "application/msword") || hasExtension(fileName, "doc");
        }

        // PEM certificate (ASCII BEGIN CERTIFICATE)
        if (containsAsciiHeader(data, "-----BEGIN CERTIFICATE-----")) {
            boolean parsed = tryParseX509(data);
            log.debug("matchesKnownSignature: detected PEM certificate, parse result: {}", parsed);
            return parsed && (containsMime(declaredContentType, "pem") || hasExtension(fileName, "pem") || hasExtension(fileName, "cer") || hasExtension(fileName, "crt"));
        }

        // DER / PKCS#12 (ASN.1 sequence typically starts with 0x30)
        if (data.length >= 1 && (data[0] & 0xFF) == 0x30) {
            log.debug("matchesKnownSignature: detected possible DER/PKCS#12 signature");
            // Try to parse as X.509 DER first
            if (tryParseX509(data)) {
                log.debug("matchesKnownSignature: X.509 DER parse succeeded");
                return containsMime(declaredContentType, "x-x509") || containsMime(declaredContentType, "pkix") || hasExtension(fileName, "der") || hasExtension(fileName, "cer") || hasExtension(fileName, "crt");
            }

            // If X.509 parsing failed, try PKCS#12
            if (tryParsePKCS12(data)) {
                log.debug("matchesKnownSignature: PKCS#12 parse succeeded");
                return containsMime(declaredContentType, "pkcs12") || containsMime(declaredContentType, "x-pkcs12") || hasExtension(fileName, "p12") || hasExtension(fileName, "pfx");
            }

            // neither parsed successfully
            return false;
        }

        // simple plaintext fallback: check if mostly readable chars
        if (isMostlyText(data)) {
            return containsMime(declaredContentType, "text/plain") || hasExtension(fileName, "txt");
        }

        // If we couldn't determine known signature, be conservative and allow when extension and mime align
        String ext = extractExtension(fileName);
        if (ext != null) {
            String mapped = EXTENSION_TO_MIME.get(ext);
            if (mapped != null && containsMime(declaredContentType, mapped)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsMime(String declared, String expected) {
        log.debug("containsMime: declared='{}', expected='{}'", declared, expected);
        if (declared == null || expected == null) return false;
        return declared.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private boolean hasExtension(String fileName, String ext) {
        String e = extractExtension(fileName);
        return e != null && e.equalsIgnoreCase(ext);
    }

    private String extractExtension(String fileName) {
        if (fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return null;
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean isMostlyText(byte[] data) {
        int readable = 0;
        for (int i = 0; i < Math.min(data.length, 200); i++) {
            int b = data[i] & 0xFF;
            if (b >= 32 && b <= 126) readable++;
        }
        log.debug("isMostlyText: readable chars: {} out of {}", readable, Math.min(data.length, 200));
        return ((double) readable / (double) Math.min(data.length, 200)) > 0.90d;
    }

    private boolean containsAsciiHeader(byte[] data, String header) {
        try {
            int len = Math.min(data.length, header.length() + 50);
            String s = new String(data, 0, len, StandardCharsets.US_ASCII);
            log.debug("returning containsAsciiHeader check for header {}: {}", header, s.contains(header));
            return s.contains(header);
        } catch (Exception e) {
            log.debug("containsAsciiHeader check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean tryParseX509(byte[] data) {
        try (InputStream in = new ByteArrayInputStream(data)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cf.generateCertificate(in);
            log.info("X.509 parse succeeded");
            return true;
        } catch (Exception e) {
            log.debug("X.509 parse failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean tryParsePKCS12(byte[] data) {
        try (InputStream in = new ByteArrayInputStream(data)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            // Try load with empty password first; many p12 files require a password and will fail
            try {
                ks.load(in, new char[0]);
                log.info("PKCS12 load succeeded with empty password");
                return true;
            } catch (Exception e) {
                log.debug("PKCS12 load with empty password failed: {}", e.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.debug("PKCS12 parse failed: {}", e.getMessage());
            return false;
        }
    }
}
