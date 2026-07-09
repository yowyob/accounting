package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChargeVentileeStatsDto {
    private BigDecimal totalIncorporable;
    private BigDecimal totalNonIncorporable;
    private BigDecimal totalVentile;
    private BigDecimal totalNonVentile;
}
