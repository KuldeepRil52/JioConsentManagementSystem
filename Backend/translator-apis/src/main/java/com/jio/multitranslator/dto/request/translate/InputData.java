package com.jio.multitranslator.dto.request.translate;

import com.jio.multitranslator.dto.request.InputItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing input data for translation request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputData {
    private List<InputItem> input;
}
