package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataTypeRequest {

    @Schema(description = "Name of the data type", example = "Email Address")
    @JsonProperty("dataTypeName")
    private String dataTypeName;
    @Schema(description = "List of data items associated with this data type", example = "[\"emailId\", \"contactEmail\"]")
    @JsonProperty("dataItems")
    private List<String> dataItems;

}
