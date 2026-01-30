package com.yowyob.erp.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrandLivreDto {
    private String noCompte;
    private String libelleCompte;
    private BigDecimal soldeOuverture;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal soldeCloture;
    private List<LigneGrandLivreDto> lignes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneGrandLivreDto {
        private UUID ecritureId;
        private LocalDateTime date;
        private String journal;
        private String reference;
        private String libelle;
        private BigDecimal debit;
        private BigDecimal credit;
    }
}