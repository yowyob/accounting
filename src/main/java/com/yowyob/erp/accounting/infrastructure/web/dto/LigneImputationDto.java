package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LigneImputationDto {
    private UUID id;
    private UUID ecritureId;
    private UUID centreId;
    private String centreLibelle;
    private UUID uniteOeuvreId;
    private String uniteOeuvreLibelle;
    private BigDecimal montant;
    private BigDecimal quantiteUo;
    @Builder.Default
    private String sens = "DEBIT";
    private String libelle;
    private UUID cleRepartitionId;
}
