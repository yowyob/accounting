package com.yowyob.erp.accounting.infrastructure.web.dto.report;

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
public class ExecutiveSummaryDto {
    private List<SummaryItemDto> bilan;
    private List<SummaryItemDto> compteResultat;
    private List<SummaryItemDto> fluxTresorerie;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryItemDto {
        private String section;
        private BigDecimal total;
        private String description;
    }
}
