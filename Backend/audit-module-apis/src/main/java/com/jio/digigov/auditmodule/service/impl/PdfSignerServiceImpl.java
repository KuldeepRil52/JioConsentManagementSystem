package com.jio.digigov.auditmodule.service.impl;

import com.jio.digigov.auditmodule.config.MultiTenantMongoConfig;
import com.jio.digigov.auditmodule.service.PdfSignerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import com.jio.digigov.auditmodule.service.dto.CertifyingOfficer;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfSignerServiceImpl implements PdfSignerService {

    private final MultiTenantMongoConfig mongoConfig;
    private static final int SIGNATURE_SIZE = 16 * 1024;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public byte[] signPdf(byte[] pdfBytes, String tenantId) throws Exception {

        // ===============================
        // 1. Load keystore from DB
        // ===============================
        MongoTemplate mongo = mongoConfig.getMongoTemplateForTenant(tenantId);

        Document cfg = mongo.findOne(
                Query.query(Criteria.where("scopeLevel").is("TENANT")),
                Document.class,
                "system_configurations"
        );

        if (cfg == null) {
            // No signing configuration found → return original PDF as-is
            return pdfBytes;
        }

        Document json = (Document) cfg.get("configurationJson");

        if (json == null) {
            return pdfBytes;
        }

        String keystoreData = json.getString("keystoreData");
        String password = json.getString("keystorePassword");
        String alias = json.getString("alias");

        if (keystoreData == null || password == null || alias == null) {
            // Config exists but signing is not properly configured
            return pdfBytes;
        }

        String base64;
        if (keystoreData.contains(",")) {
            base64 = keystoreData.substring(keystoreData.indexOf(",") + 1);
        } else {
            base64 = keystoreData;
        }

        // Remove whitespace and newlines
        base64 = base64.replaceAll("\\s+", "");

        byte[] keystoreBytes = Base64.getDecoder().decode(base64);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(keystoreBytes), password.toCharArray());

        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);

        X509Certificate cert = (X509Certificate) chain[0];
        String signerName = getCN(cert);
        log.info("Signing pdf data is : {}", cert.getSubjectX500Principal().getName());

        // ===============================
        // 2. Load PDF
        // ===============================
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            PDAcroForm acroForm = Optional.ofNullable(
                    document.getDocumentCatalog().getAcroForm()
            ).orElseGet(() -> {
                PDAcroForm f = new PDAcroForm(document);
                document.getDocumentCatalog().setAcroForm(f);
                return f;
            });

            PDPage page = document.getPage(document.getNumberOfPages() - 1);

            float x = 50, y = 30, w = 280, h = 80;
            PDRectangle rect = new PDRectangle(x, y, w, h);

            PDSignatureField sigField = new PDSignatureField(acroForm);
            PDAnnotationWidget widget = sigField.getWidgets().get(0);
            widget.setRectangle(rect);
            widget.setPage(page);
            page.getAnnotations().add(widget);

            sigField.setPartialName("Signature1");
            acroForm.getFields().add(sigField);

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(signerName);
            signature.setReason("Document Integrity Verification");
            signature.setLocation("India");
            signature.setSignDate(Calendar.getInstance());

            sigField.setValue(signature);

            // ===============================
            // 3. DRAW VISUAL SIGNATURE (OLD WAY)
            // ===============================
            try (PDPageContentStream cs = new PDPageContentStream(
                    document, page,
                    PDPageContentStream.AppendMode.APPEND,
                    true, true)) {

                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.addRect(x, y, w, h);
                cs.fill();

                cs.setStrokingColor(0, 0, 0);
                cs.setLineWidth(1.5f);
                cs.addRect(x, y, w, h);
                cs.stroke();

                cs.setNonStrokingColor(0, 0, 0);

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);
                cs.newLineAtOffset(x + 5, y + h - 15);
                cs.showText("Digitally Signed by:");
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                cs.newLineAtOffset(x + 5, y + h - 30);
                cs.showText(signerName);
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 7);
                cs.newLineAtOffset(x + 5, y + h - 45);
                cs.showText("Date: " + signature.getSignDate().getTime());
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 7);
                cs.newLineAtOffset(x + 5, y + h - 60);
                cs.showText("Reason: " + signature.getReason());
                cs.endText();
            }

            // ===============================
            // 4. Cryptographic signing
            // ===============================
            SignatureOptions opts = new SignatureOptions();
            opts.setPreferredSignatureSize(SIGNATURE_SIZE);

            document.addSignature(
                    signature,
                    new BcPKCS7Signer(privateKey, chain),
                    opts
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.saveIncremental(out);
            return out.toByteArray();
        }
    }

    private String getCN(X509Certificate cert) {
        for (String p : cert.getSubjectX500Principal().getName().split(",")) {
            if (p.trim().startsWith("CN=")) return p.trim().substring(3);
        }
        return "CMS";
    }

    private static class BcPKCS7Signer implements SignatureInterface {
        private final PrivateKey key;
        private final Certificate[] chain;

        BcPKCS7Signer(PrivateKey key, Certificate[] chain) {
            this.key = key;
            this.chain = chain;
        }

        @Override
        public byte[] sign(InputStream content) throws IOException {
            try {
                byte[] data = content.readAllBytes();
                List<X509Certificate> certs = new ArrayList<>();
                for (Certificate c : chain) certs.add((X509Certificate) c);

                CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                gen.addCertificates(new JcaCertStore(certs));

                ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                        .setProvider("BC").build(key);

                gen.addSignerInfoGenerator(
                        new JcaSignerInfoGeneratorBuilder(
                                new JcaDigestCalculatorProviderBuilder()
                                        .setProvider("BC").build()
                        ).build(signer, certs.get(0))
                );

                return gen.generate(
                        new CMSProcessableByteArray(data),
                        false
                ).getEncoded();

            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public CertifyingOfficer getCertifyingOfficer(String tenantId) throws Exception {

        MongoTemplate mongo = mongoConfig.getMongoTemplateForTenant(tenantId);

        Document cfg = mongo.findOne(
                Query.query(Criteria.where("scopeLevel").is("TENANT")),
                Document.class,
                "system_configurations"
        );

        if (cfg == null) {
            return defaultOfficer();
        }

        Document json = (Document) cfg.get("configurationJson");
        if (json == null) {
            return defaultOfficer();
        }

        String keystoreData = json.getString("keystoreData");
        String password = json.getString("keystorePassword");
        String alias = json.getString("alias");

        if (keystoreData == null || password == null || alias == null) {
            return defaultOfficer();
        }

        // FIX: use safe loader
        byte[] ksBytes = loadKeystoreBytes(keystoreData);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(ksBytes), password.toCharArray());

        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        return new CertifyingOfficer(
                getValue(cert, "CN"),
                getValue(cert, "OU") != null ? getValue(cert, "OU") : "Digital Evidence Custodian",
                getValue(cert, "O")  != null ? getValue(cert, "O")  : "DataTrust Technologies Pvt. Ltd.",
                "legal@datatrust.example"
        );
    }

    private CertifyingOfficer defaultOfficer() {
        return new CertifyingOfficer(
                "CMS",
                "Digital Evidence Custodian",
                "DataTrust Technologies Pvt. Ltd.",
                "legal@datatrust.example"
        );
    }

    private String getValue(X509Certificate cert, String key) {
        for (String part : cert.getSubjectX500Principal().getName().split(",")) {
            part = part.trim();
            if (part.startsWith(key + "=")) {
                return part.substring(key.length() + 1);
            }
        }
        return null;
    }

    private byte[] loadKeystoreBytes(String keystoreData) throws IOException {

        if (keystoreData == null || keystoreData.isBlank()) {
            throw new IOException("Keystore data is empty");
        }

        String trimmed = keystoreData.trim();

        // Case 1: File path
        if (trimmed.startsWith("file:") ||
                trimmed.startsWith("/") ||
                trimmed.matches("^[A-Za-z]:\\\\.*")) {

            String path = trimmed.replace("file:", "");
            return java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(path)
            );
        }

        //  Case 2: Base64 (existing working logic)
        String base64 = trimmed.contains(",")
                ? trimmed.substring(trimmed.indexOf(",") + 1)
                : trimmed;

        base64 = base64.replaceAll("\\s+", "");
        return Base64.getDecoder().decode(base64);
    }
}
