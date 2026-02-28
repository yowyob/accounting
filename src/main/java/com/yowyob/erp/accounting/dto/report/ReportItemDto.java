package com.yowyob.erp.accounting.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportItemDto {
    private String code;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal solde;
}
