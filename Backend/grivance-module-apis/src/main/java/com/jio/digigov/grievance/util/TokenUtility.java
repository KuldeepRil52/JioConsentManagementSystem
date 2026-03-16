package com.jio.digigov.grievance.util;

import com.jio.digigov.grievance.dto.response.GrievanceTokenValidateResponse;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenUtility {
    public static RSAKey loadRSAKey() throws Exception {
        try (InputStream is = TokenUtility.class.getResourceAsStream("/jwt-set.json")) {
            assert is != null;
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JWK jwk = JWK.parse(json);
            return (RSAKey) jwk;
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

    public GrievanceTokenValidateResponse verifyConsentToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        RSAKey rsaKey = loadRSAKey();

        JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

        if (!signedJWT.verify(verifier)) {
            return new GrievanceTokenValidateResponse("Invalid Consent Token");
        }

        if (signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date())) {
            return new GrievanceTokenValidateResponse("Expired Consent Token");
        }

        return new GrievanceTokenValidateResponse("Valid Consent Token");
    }
}
