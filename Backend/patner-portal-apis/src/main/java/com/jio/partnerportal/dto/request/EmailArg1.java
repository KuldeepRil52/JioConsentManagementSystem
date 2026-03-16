package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailArg1 {
    @JsonProperty("<#ARG2>")
    private String arg2;

    public EmailArg1(String user, String partner) {
        this.arg2 = partner;

    }
}
