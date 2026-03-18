package com.jio.partnerportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCatagory {

    private String dataTypeId;
    private String dataTypeName;
    private List<String> dataItems;

}
