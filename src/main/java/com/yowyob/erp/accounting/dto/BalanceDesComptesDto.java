package com.yowyob.erp.accounting.dto;

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
public class BalanceDesComptesDto {
    private BigDecimal totalDebitOuverture;
    private BigDecimal totalCreditOuverture;
    private BigDecimal totalDebitMouvement;
    private BigDecimal totalCreditMouvement;
    private BigDecimal totalDebitCloture;
    private BigDecimal totalCreditCloture;
    private List<LigneBalanceDto> lignes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneBalanceDto {
        private String noCompte;
        private String libelle;
        private BigDecimal soldeOuvertureDebit;
        private BigDecimal soldeOuvertureCredit;
        private BigDecimal mouvementDebit;
        private BigDecimal mouvementCredit;
        private BigDecimal soldeClotureDebit;
        private BigDecimal soldeClotureCredit;
    }
}
