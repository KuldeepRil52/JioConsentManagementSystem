package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
	
	@Schema(description = "Font file as an base64 encoded string", example = "AAEA+wABAPwAAQD9AAEA/gACAP8BAAABAQEAAQECAAEBAwABAQQAAQEFAAEBBgABAQcAAwEQAREBHgADARIBHwEgAAIBEwEhAAMBFAEiASMAAgEVASQAAQElAAEBJgACAScBKAABASkAAQEqAAIBFgErAAEBLAABAS0AAgEXAS4AAQEYAAEBLwACARkBMAABATEAAgEaATIAAgEzATQAAwEcATUBNgABARsAAQE3AAEBHQACAAQABAAMAAAADgAdAAkAHwAtABkAMAA4ACgAAgBoADEA6gDsAO0A7wDwAPEA8gD0APUA9gD4APkA+gD7APwA/QD+AP8BAQECAQMBBAEFAQYBBwEQARIBEwEUARUBJQEmAScBKQEqARYBLAEtARcBGAEvARkBMQEaATMBHAEbATcBHQACAAQABAAMAAAADgAdAAkAHwAtABkAMAA4ACgAAgAoABEA6wDuAPMA9wEAAREBHwEhASIBJAEoASsBLgEwATIBNAE1AAEAEQAEAAYACgAOABYAHwAgACEAIgAjACYAKQAsADEAMwA0ADUAAgAOAAQBHgEgASMBNgABAAQAHwAgACIANQABAJIACQAYACoAPABGAGAAagB0AH4AiAACAAYADAEIAAIABAEJAAIACwACAAYADAEKAAIABAELAAIACwABAAQBDwACAAgAAwAIAA4AFAEMAAIABAEOAAIADAENAAIADwABAAQBPQACADIAAQAEAT4AAgAyAAEABAE/AAIAJwABAAQBQAACACcAAQAEAUEAAgAnAAEACQAGAAoACwAPACEAMQA0ADUANwABAEIAAQAIAAcAEAAWABwAIgAoAC4ANAE4AAIAIAE5AAIAJAE6AAIAJgCfAAIAJwE7AAIAKAE8AAIAKQCgAAIAKgABAAEAJAACAA4ABAB/AIAAfwCAAAEABAAEABIAHwAtAAAAAQAAAAA=")
    @JsonProperty("fontFile")
    private String fontFile;
    
    @Schema(description = "font size", example = "14")
    @JsonProperty("fontSize")
    private Integer fontSize;
    
    @Schema(description = "font weight", example = "200")
    @JsonProperty("fontWeight")
    private Integer fontWeight;
    
    @Schema(description = "font style", example = "Bold")
    @JsonProperty("fontStyle")
    private String fontStyle;
}
