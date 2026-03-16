package com.jio.digigov.fides.entity;

import lombok.Data;

import java.util.List;

@Data
public class WithdrawalData {

    private DataFiduciary dataFiduciary;
    private List<PIIItem> piiItems;
}