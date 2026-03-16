package com.jio.vault.util;

import com.jio.vault.client.VaultClient;
import com.jio.vault.constants.ErrorCode;
import com.jio.vault.exception.CustomException;
import org.springframework.stereotype.Component;

import java.util.Base64;


import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;


import java.io.*;

@Component
public class FileSigner {

    private final VaultClient vaultClient;

    public FileSigner(VaultClient vaultClient) {
        this.vaultClient = vaultClient;
    }

    public String signFile(byte[] fileBytes, String keyName) throws CustomException {
        try {
            String vaultInput = Base64.getEncoder().encodeToString(fileBytes);
            String requestBody = String.format(
                    "{ \"input\": \"%s\", \"signature_algorithm\": \"pkcs1v15\" }",
                    vaultInput
            );
            String vaultSignature = vaultClient.sign(requestBody, keyName);

            if (vaultSignature.startsWith("vault:")) {
                vaultSignature = vaultSignature.substring(vaultSignature.lastIndexOf(':') + 1);
            }
            return vaultSignature;

        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "Failed to sign file: " + e.getMessage());
        }
    }
    public byte[] signPdfWithSignature(byte[] pdfBytes, String keyName) throws CustomException {
        String base64Signature = signFile(pdfBytes, keyName);
        try (
                ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            PdfReader reader = new PdfReader(inputStream);
            PdfStamper stamper = new PdfStamper(reader, outputStream);

            // Create visible signature stamp on the first page
            PdfContentByte content = stamper.getOverContent(1);
            Font font = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
            String signatureText = "Digitally signed by Jio Vault\nSignature: "
                    + base64Signature.substring(0, Math.min(80, base64Signature.length())) + "...";
            Phrase phrase = new Phrase(signatureText, font);

            Rectangle rect = reader.getPageSize(1);
            float x = rect.getLeft() + 36;
            float y = rect.getBottom() + 36;
            ColumnText.showTextAligned(content, Element.ALIGN_LEFT, phrase, x, y, 0);

            stamper.close();
            reader.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "Failed to sign PDF: " + e.getMessage());
        }
    }
}
