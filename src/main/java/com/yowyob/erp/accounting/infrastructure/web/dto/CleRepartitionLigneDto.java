package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CleRepartitionLigneDto {
    private UUID id;
    private UUID cleId;
    private UUID centreDestinataireId;
    private String centreDestinataireLibelle;
    private BigDecimal pourcentage;
    private UUID uniteOeuvreId;
    private String uniteOeuvreLibelle;
}
