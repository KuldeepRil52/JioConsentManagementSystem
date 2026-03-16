package com.jio.consent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LanguageTypographySettings {
	
	@Schema(description = "font file in base64", example = "Base64of TTF/OTF/WOFF")
    @NotBlank(message = ErrorCodes.JCMP1061)
	private String fontFile;
	
	@Schema(description = "font size", example = "12")
    @NotNull(message = ErrorCodes.JCMP1062)
	private Integer fontSize;
	
	@Schema(description = "font weight", example = "200")
    @NotNull(message = ErrorCodes.JCMP1063)
	private Integer fontWeight;
	
	@Schema(description = "font style", example = "BOLD")
    @NotBlank(message = ErrorCodes.JCMP1064)
	private String fontStyle;
}
