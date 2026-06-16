package com.jio.auth.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;


@Setter
@Getter
@Document(collection = "active_tokens")
public class ActiveToken {

    @Id
    private String jti;
    private String userId;

    @Indexed(name = "ttl_index", expireAfter = "P1D")
    private Instant expiry;

    public ActiveToken() {}

    public ActiveToken(String jti, String userId, Instant expiry) {
        this.jti = jti;
        this.userId = userId;
        this.expiry = expiry;
    }

}
