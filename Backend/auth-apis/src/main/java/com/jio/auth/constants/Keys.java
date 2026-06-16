package com.jio.auth.constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class Keys {

    @Value("${privateKey:private-key.json}")
    private String privateKey;

    @Value("${publicKey:private-key.json}")
    private String publicKey;

    public String getPrivateKey() {
        return privateKey;
    }
    public String getPublicKey() {return publicKey;}
}
