package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.jio.partnerportal.dto.IdentityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class OtpInitRequest{

        @JsonProperty("idValue")
        private String idValue;

        @JsonProperty("idType")
        @JsonSetter(nulls = Nulls.AS_EMPTY, contentNulls = Nulls.AS_EMPTY)
        private IdentityType idType;

}
