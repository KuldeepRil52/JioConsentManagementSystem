package com.example.scanner.dto.response;

import com.example.scanner.dto.Preference;
import com.example.scanner.entity.CookieEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Preference with associated cookies")
public class PreferenceWithCookies {

    @JsonUnwrapped  // This flattens all Preference fields at the same level
    @Schema(hidden = true)
    private Preference preference;

    @Schema(
            description = "List of cookies associated with this preference category",
            implementation = CookieEntity.class
    )
    private List<CookieEntity> cookies;

    // Helper constructor to create from Preference
    public static PreferenceWithCookies from(Preference preference, List<CookieEntity> cookies) {
        PreferenceWithCookies result = new PreferenceWithCookies();
        result.setPreference(preference);
        result.setCookies(cookies);
        return result;
    }
}