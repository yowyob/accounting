package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ActiviteNormaleDto {
    private UUID centreId;
    private String centreLibelle;
    private BigDecimal activiteNormale;
    private String unite;
}
