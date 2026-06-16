package com.example.scanner.util;

import com.example.scanner.dto.response.ConsentTokenValidateResponse;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Date;

@Component
public class TokenUtility {
    public static RSAKey loadRSAKey() throws Exception {
        try (InputStream is = TokenUtility.class.getResourceAsStream("/jwt-set.json")) {
            if (is == null) {
                throw new IllegalStateException("jwt-set.json file not found in classpath resources");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JWK jwk = JWK.parse(json);
            return (RSAKey) jwk;
        } catch (Exception e) {
            throw new Exception("Unable to load JWT signing key: " + e.getMessage(), e);
        }
    }

    public String generateToken(String data, Date expiryDate) throws Exception {
        RSAKey rsaKey = loadRSAKey();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(data)
                .issuer("JIO CONSENT")
                .issueTime(new Date())
                .expirationTime(expiryDate)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims
        );

        signedJWT.sign(new RSASSASigner(rsaKey));

        return signedJWT.serialize();
    }

    public ConsentTokenValidateResponse verifyConsentToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        RSAKey rsaKey = loadRSAKey();

        JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

        if (!signedJWT.verify(verifier)) {
            return new ConsentTokenValidateResponse("Invalid Consent Token");
        }

        Date now = new Date();
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        if (now.after(expirationTime)) {
            return new ConsentTokenValidateResponse("Expired Consent Token");
        }

        return new ConsentTokenValidateResponse("Valid Consent Token");
    }

}
