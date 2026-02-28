package com.yowyob.erp.accounting.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowDto {
    private List<CashFlowItemDto> operationnel;
    private List<CashFlowItemDto> investissement;
    private List<CashFlowItemDto> financement;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlowItemDto {
        private String code;
        private String description;
        private BigDecimal amount;
        private String category;
    }
}
