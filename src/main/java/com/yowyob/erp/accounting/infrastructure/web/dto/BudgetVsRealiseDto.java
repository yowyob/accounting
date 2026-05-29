package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetVsRealiseDto {

    private UUID exerciceId;
    private String exerciceCode;
    private List<LigneBudgetVsRealiseDto> lignes;

    private BigDecimal totalBudget;
    private BigDecimal totalRealise;
    private BigDecimal totalEcart;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneBudgetVsRealiseDto {
        private String noCompte;
        private String libelleCompte;
        private BigDecimal montantBudget;
        private BigDecimal montantRealise;
        /** Réalisé − Budget (positif = dépassement pour charges, sous-réalisation pour produits) */
        private BigDecimal ecart;
        /** Taux de réalisation : réalisé / budget * 100 */
        private BigDecimal tauxRealisation;
    }
}
