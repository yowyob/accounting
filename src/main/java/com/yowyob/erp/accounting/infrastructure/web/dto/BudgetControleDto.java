package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BudgetControleDto {
    private UUID budgetId;
    private String nom;
    private BigDecimal montantAlloue;
    private BigDecimal montantRealise;
    private BigDecimal montantEcart;
    private BigDecimal pourcentageConsomme;
    private Boolean seuilAlerteAtteint;
    private List<LigneControleDto> lignes;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LigneControleDto {
        private UUID compteId;
        private String noCompte;
        private String libelleCompte;
        private BigDecimal montantAlloue;
        private BigDecimal montantRealise;
        private BigDecimal montantEcart;
        private BigDecimal pourcentageConsomme;
    }
}
