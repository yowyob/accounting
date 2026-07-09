package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConcordanceCalculDto {
    private UUID periodeId;
    private String periodeCgLibelle;
    private BigDecimal resultCG;
    private BigDecimal totalChargesCG;
    private BigDecimal totalProduitsCG;
    private BigDecimal totalNonInc;
    private BigDecimal totalIncorporable;
    private BigDecimal totalAnalytiqueEcritures;
    private BigDecimal sommeDiff;
    private BigDecimal resultCA;
    private BigDecimal ecartVerif;
    private Boolean concordanceOk;
    private List<LigneConcordanceDto> lignesManuelles;
    private List<LigneConcordanceDto> lignesAuto;
    private List<LigneConcordanceDto> lignes;
}
