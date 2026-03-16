package com.jio.consent.utils;

import com.jio.consent.constant.Constants;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.DocumentMeta;
import com.jio.consent.dto.LANGUAGE;
import com.jio.consent.dto.LanguageTypographySettings;
import com.jio.consent.exception.BodyValidationException;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.repository.TenantRegistryRepository;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.ThreadContext;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.nio.charset.StandardCharsets;

@Component
public class Validation {

    TenantRegistryRepository tenantRegistryRepository;
    
    private static final Set<String> ALLOWED_FONT_TYPES = Set.of(
            "font/ttf",
            "font/otf",
            "font/woff",
            "font/woff2",
            "application/x-font-ttf",
            "application/x-font-otf",
            "application/font-woff"
    );

    public Validation(TenantRegistryRepository tenantRegistryRepository) {
        this.tenantRegistryRepository = tenantRegistryRepository;
    }

    @Value("${file.max.size.bytes}")
    private Long maxFileSize;

    @Value("${file.allowed.document.types}")
    private List<String> allowedDocumentTypes;

    public void validateTenantIdHeader() throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (ObjectUtils.isEmpty(ThreadContext.get(Constants.TENANT_ID_HEADER)) || !this.tenantRegistryRepository.existsByTenantId(ThreadContext.get(Constants.TENANT_ID_HEADER))) {
            Map<String, Object> error = getHeaderErrorDetails(ErrorCodes.JCMP2001, Constants.TENANT_ID_HEADER);
            errs.add(error);
        }
        if (!ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }

    private Map<String, Object> getHeaderErrorDetails(String errorCode, String header) {
        Map<String, Object> errMap = new HashMap<>();
        errMap.put(Constants.ERROR_CODE, errorCode);
        errMap.put(Constants.HEADER, header);
        return errMap;
    }

    public void validateTxnHeader(String txn) throws BodyValidationException {
        List<Map<String, Object>> errs = new ArrayList<>();
        if (org.springframework.util.ObjectUtils.isEmpty(txn)) {
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
        if (!org.springframework.util.ObjectUtils.isEmpty(errs)) {
            throw new BodyValidationException(errs);
        }
    }


    public void validateDocument(String dataUri, DocumentMeta documentMeta) throws ConsentException {

        if (dataUri == null || dataUri.trim().isEmpty() || !dataUri.startsWith("data:")) {
            throw new ConsentException(ErrorCodes.JCMP1038);
        }

        String[] dataParts = dataUri.split(",", 2);
        if (dataParts.length != 2) {
            throw new ConsentException(ErrorCodes.JCMP1038);
        }

        String header = dataParts[0];
        String base64Content = dataParts[1];

        if (!header.contains(";base64")) {
            throw new ConsentException(ErrorCodes.JCMP1038);
        }

        String mimeType = header.substring(5, header.indexOf(";base64"));

        if( !documentMeta.getContentType().equals(mimeType)) {
            throw new ConsentException(ErrorCodes.JCMP1040);
        }
        if (!allowedDocumentTypes.contains(mimeType)) {
            throw new ConsentException(ErrorCodes.JCMP1039);
        }

        base64Content = base64Content.trim().replaceAll("\\s", "");

        byte[] decodedBytes;
        long calculatedSize;
        try {
            decodedBytes = Base64.getDecoder().decode(base64Content);
            calculatedSize = decodedBytes.length;
        } catch (IllegalArgumentException e) {
            throw new ConsentException(ErrorCodes.JCMP1038);
        }

        // Size validation: first check exact match, then allow 10% variance
        if (documentMeta.getSize() != null) {
            if (documentMeta.getSize() != calculatedSize) {
                // Allow 10% variance or 10 bytes difference
                long diff = Math.abs(calculatedSize - documentMeta.getSize());
                double pct = (double) diff / (double) Math.max(1, documentMeta.getSize());
                if (pct > 0.10d && diff > 10) {
                    throw new ConsentException(ErrorCodes.JCMP1036);
                }
            }
        }

        if (calculatedSize > maxFileSize) {
            throw new ConsentException(ErrorCodes.JCMP1037);
        }

        // Validate file extension matches content type
        String fileName = documentMeta.getName();
        if (fileName != null && !fileName.trim().isEmpty()) {
            String extension = extractExtension(fileName);
            if (extension != null && !isExtensionValidForContentType(extension, mimeType)) {
                throw new ConsentException(ErrorCodes.JCMP1040);
            }
        }

        // Validate file signature (magic bytes) matches declared content type
        if (!matchesFileSignature(decodedBytes, mimeType, fileName)) {
            throw new ConsentException(ErrorCodes.JCMP1040);
        }
    }

    /**
     * Extracts file extension from filename
     */
    private String extractExtension(String fileName) {
        if (fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return null;
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Validates if file extension is valid for the given content type
     */
    private boolean isExtensionValidForContentType(String extension, String contentType) {
        if (extension == null || contentType == null) return true; // Skip validation if missing
        
        String lowerContentType = contentType.toLowerCase(Locale.ROOT);
        String lowerExtension = extension.toLowerCase(Locale.ROOT);
        
        // PDF
        if (lowerExtension.equals("pdf")) {
            return lowerContentType.contains("application/pdf");
        }
        // PNG
        if (lowerExtension.equals("png")) {
            return lowerContentType.contains("image/png");
        }
        // JPEG
        if (lowerExtension.equals("jpg") || lowerExtension.equals("jpeg")) {
            return lowerContentType.contains("image/jpeg");
        }
        // DOCX
        if (lowerExtension.equals("docx")) {
            return lowerContentType.contains("vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                   lowerContentType.contains("application/zip"); // DOCX is a ZIP file
        }
        // DOC
        if (lowerExtension.equals("doc")) {
            return lowerContentType.contains("application/msword");
        }
        // ZIP
        if (lowerExtension.equals("zip")) {
            return lowerContentType.contains("application/zip");
        }
        // TXT
        if (lowerExtension.equals("txt")) {
            return lowerContentType.contains("text/plain");
        }
        // Certificate types
        if (lowerExtension.equals("cer") || lowerExtension.equals("crt")) {
            return lowerContentType.contains("pkix-cert") || lowerContentType.contains("x-x509");
        }
        if (lowerExtension.equals("pem")) {
            return lowerContentType.contains("pem") || lowerContentType.contains("pkix-cert") || 
                   lowerContentType.contains("x-x509");
        }
        if (lowerExtension.equals("der")) {
            return lowerContentType.contains("pkix-cert") || lowerContentType.contains("x-x509");
        }
        if (lowerExtension.equals("p12") || lowerExtension.equals("pfx")) {
            return lowerContentType.contains("pkcs12") || lowerContentType.contains("x-pkcs12");
        }
        
        // If extension is not in our known list, allow it (don't fail validation)
        return true;
    }

    /**
     * Validates file signature (magic bytes) matches the declared content type
     */
    private boolean matchesFileSignature(byte[] data, String declaredContentType, String fileName) {
        if (data == null || data.length == 0) {
            return false;
        }

        String lowerContentType = declaredContentType != null ? 
            declaredContentType.toLowerCase(Locale.ROOT) : "";

        // PDF signature: %PDF
        if (startsWith(data, "%PDF".getBytes(StandardCharsets.US_ASCII))) {
            return lowerContentType.contains("application/pdf") || 
                   (fileName != null && hasExtension(fileName, "pdf"));
        }

        // PNG signature: 89 50 4E 47
        if (data.length >= 8 && (data[0] & 0xFF) == 0x89 && data[1] == 0x50 && 
            data[2] == 0x4E && data[3] == 0x47) {
            return lowerContentType.contains("image/png") || 
                   (fileName != null && hasExtension(fileName, "png"));
        }

        // JPEG signature: FF D8 FF
        if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && 
            (data[2] & 0xFF) == 0xFF) {
            return lowerContentType.contains("image/jpeg") || 
                   (fileName != null && (hasExtension(fileName, "jpg") || hasExtension(fileName, "jpeg")));
        }

        // ZIP / OOXML (DOCX) signature: PK 03 04
        if (data.length >= 4 && data[0] == 'P' && data[1] == 'K' && 
            data[2] == 3 && data[3] == 4) {
            return lowerContentType.contains("application/zip") || 
                   lowerContentType.contains("vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                   (fileName != null && (hasExtension(fileName, "zip") || hasExtension(fileName, "docx")));
        }

        // MS-OLE Compound File (older .doc): D0 CF 11 E0
        if (data.length >= 8 && (data[0] & 0xFF) == 0xD0 && (data[1] & 0xFF) == 0xCF && 
            (data[2] & 0xFF) == 0x11 && (data[3] & 0xFF) == 0xE0) {
            return lowerContentType.contains("application/msword") || 
                   (fileName != null && hasExtension(fileName, "doc"));
        }

        // PEM certificate: ASCII "-----BEGIN CERTIFICATE-----"
        if (containsAsciiHeader(data, "-----BEGIN CERTIFICATE-----")) {
            return lowerContentType.contains("pem") || lowerContentType.contains("pkix-cert") || 
                   lowerContentType.contains("x-x509") ||
                   (fileName != null && (hasExtension(fileName, "pem") || hasExtension(fileName, "cer") || 
                                         hasExtension(fileName, "crt")));
        }

        // DER / PKCS#12: ASN.1 sequence typically starts with 0x30
        if (data.length >= 1 && (data[0] & 0xFF) == 0x30) {
            // For DER/PKCS#12, we'll be lenient if content type matches
            if (lowerContentType.contains("pkix-cert") || lowerContentType.contains("x-x509") || 
                lowerContentType.contains("pkcs12") || lowerContentType.contains("x-pkcs12")) {
                return true;
            }
            if (fileName != null) {
                String ext = extractExtension(fileName);
                if (ext != null && (ext.equals("der") || ext.equals("cer") || ext.equals("crt") || 
                                    ext.equals("p12") || ext.equals("pfx"))) {
                    return true;
                }
            }
        }

        // Plain text fallback: check if mostly readable characters
        if (isMostlyText(data)) {
            return lowerContentType.contains("text/plain") || 
                   (fileName != null && hasExtension(fileName, "txt"));
        }

        // If we couldn't determine signature, allow if extension and content type align
        if (fileName != null) {
            String ext = extractExtension(fileName);
            if (ext != null) {
                return isExtensionValidForContentType(ext, declaredContentType);
            }
        }

        // Conservative: if we can't validate signature, allow it (don't fail)
        return true;
    }

    /**
     * Checks if byte array starts with given prefix
     */
    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    /**
     * Checks if byte array contains ASCII header string
     */
    private boolean containsAsciiHeader(byte[] data, String header) {
        try {
            int len = Math.min(data.length, header.length() + 50);
            String s = new String(data, 0, len, StandardCharsets.US_ASCII);
            return s.contains(header);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if file has given extension
     */
    private boolean hasExtension(String fileName, String ext) {
        String e = extractExtension(fileName);
        return e != null && e.equalsIgnoreCase(ext);
    }

    /**
     * Checks if data is mostly readable text
     */
    private boolean isMostlyText(byte[] data) {
        int readable = 0;
        for (int i = 0; i < Math.min(data.length, 200); i++) {
            int b = data[i] & 0xFF;
            if (b >= 32 && b <= 126) readable++;
        }
        return ((double) readable / (double) Math.min(data.length, 200)) > 0.90d;
    }
    
    public void validateTypographySettings(Map<LANGUAGE, LanguageTypographySettings> typographySettings) throws ConsentException {
    	if(typographySettings.isEmpty()) {
    		throw new ConsentException(ErrorCodes.JCMP1060);
    	}
    	for(Map.Entry<LANGUAGE, LanguageTypographySettings> entry: typographySettings.entrySet()) {
    		LanguageTypographySettings currSettings = typographySettings.get(entry.getKey());
    		if(!Utils.isValidBase64(currSettings.getFontFile())){
    			throw new ConsentException(ErrorCodes.JCMP1065);
    		}else {
    			String base64File = currSettings.getFontFile();
    			byte[] decodedBytes = Base64.getDecoder().decode(base64File);
    	        String mimeType = new Tika().detect(decodedBytes);
    	        System.out.println(mimeType);
    	        if (!ALLOWED_FONT_TYPES.contains(mimeType)) {
    	        	throw new ConsentException(ErrorCodes.JCMP1065);
    	        }
    			
    		}
    		if(currSettings.getFontSize()==null) {
    			throw new ConsentException(ErrorCodes.JCMP1062);
    		}
    		if(currSettings.getFontWeight()==null) {
    			throw new ConsentException(ErrorCodes.JCMP1063);
    		}
    		if(currSettings.getFontStyle()==null) {
    			throw new ConsentException(ErrorCodes.JCMP1064);
    		}
    	}
    }
}
