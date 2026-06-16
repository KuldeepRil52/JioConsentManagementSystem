package com.jio.partnerportal.entity;

import com.jio.partnerportal.dto.IdentityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp")
public class Otp {

    @Id
    private String id;
    private String idValue;
    private IdentityType idType;
    private String txnId;
    private int retryCount;
    private String otpTxnId;
    private String eventId;
    @Indexed(expireAfter = "#{@environment.getProperty('otp.time.to.live')}")
    private Instant createdAt;
    private Instant validatedAt;

    public Otp(String idValue, IdentityType idType, String txnId) {
        this.idValue = idValue;
        this.idType = idType;
        this.txnId = txnId;
        this.retryCount = 0;
        this.createdAt = Instant.now();
        this.validatedAt = null;

    }

}