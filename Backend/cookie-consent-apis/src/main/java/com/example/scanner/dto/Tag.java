package com.example.scanner.dto;

import com.example.scanner.enums.Component;
import com.example.scanner.enums.DocumentTag;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tag {
    Component component;
    DocumentTag documentTag;
}