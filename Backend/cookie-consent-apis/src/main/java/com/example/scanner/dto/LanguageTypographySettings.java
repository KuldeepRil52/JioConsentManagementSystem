package com.example.scanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
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
//    @NotBlank(message = ErrorCodes.JCMP1053)
	private String fontFile;
	
	@Schema(description = "font size", example = "12")
//    @NotBlank(message = ErrorCodes.JCMP1054)
	private Integer fontSize;
	
	@Schema(description = "font weight", example = "200")
//    @NotBlank(message = ErrorCodes.JCMP1055)
	private Integer fontWeight;
	
	@Schema(description = "font style", example = "BOLD")
//    @NotBlank(message = ErrorCodes.JCMP1056)
	private String fontStyle;
}
