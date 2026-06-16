package com.jio.partnerportal.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

public class TotpUtils {

    private TotpUtils() {
    }

    // ---- Step 1: Generate Secret ----
    public static String generateSecret() {
        byte[] buffer = new byte[20]; // 160-bit random bytes
        new SecureRandom().nextBytes(buffer);
        Base32 base32 = new Base32();
        return base32.encodeToString(buffer).replace("=", "");
    }

    // ---- Step 2: TOTP Generation ----
    public static String generateTOTP(String base32Secret, long timeMillis) throws Exception {
        Base32 base32 = new Base32();
        byte[] key = base32.decode(base32Secret);

        long counter = timeMillis / 1000L / 30; // 30-second step
        byte[] data = ByteBuffer.allocate(8).putLong(counter).array();

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xf;
        int binary =
                ((hash[offset] & 0x7f) << 24) |
                        ((hash[offset + 1] & 0xff) << 16) |
                        ((hash[offset + 2] & 0xff) << 8) |
                        (hash[offset + 3] & 0xff);

        int otp = binary % 1_000_000; // 6 digits
        return String.format("%06d", otp);
    }

    // ---- Step 3: Verification ----
    public static boolean verifyTOTP(String secret, String token, int window) throws Exception {
        long now = System.currentTimeMillis();
        for (int i = -window; i <= window; i++) {
            String candidate = generateTOTP(secret, now + (i * 30_000L));
            if (candidate.equals(token)) {
                return true;
            }
        }
        return false;
    }

    // ---- Step 4: Generate TOTP Provisioning URI ----
    /**
     * Generates a TOTP provisioning URI for Google Authenticator
     * 
     * @param secretKey The Base32 encoded secret key
     * @param accountName The account name (e.g., email or username)
     * @param issuer The issuer name (e.g., "JCMP_PARTNERPORTAL")
     * @return The otpauth:// URI string
     */
    public static String generateTOTPProvisioningURI(String secretKey, String accountName, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, accountName, secretKey, issuer);
    }

    // ---- Step 5: Generate QR Code as Base64 ----
    /**
     * Generates a QR Code as Base64 encoded PNG image
     * 
     * @param text The text to encode in the QR code
     * @param width The width of the QR code image
     * @param height The height of the QR code image
     * @return Base64 encoded PNG image string
     * @throws WriterException If QR code generation fails
     * @throws java.io.IOException If image encoding fails
     */
    public static String generateQRCodeBase64(String text, int width, int height) 
            throws WriterException, java.io.IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int grayValue = (bitMatrix.get(x, y) ? 0 : 0xFFFFFF);
                image.setRGB(x, y, grayValue);
            }
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Generates a QR Code as Base64 encoded PNG image with default size (250x250)
     * 
     * @param text The text to encode in the QR code
     * @return Base64 encoded PNG image string
     * @throws WriterException If QR code generation fails
     * @throws java.io.IOException If image encoding fails
     */
    public static String generateQRCodeBase64(String text) 
            throws WriterException, java.io.IOException {
        return generateQRCodeBase64(text, 250, 250);
    }
}

