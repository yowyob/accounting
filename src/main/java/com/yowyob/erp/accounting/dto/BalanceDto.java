// DTO pour les rapports de balance
package com.yowyob.erp.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDto {

    private String organizationId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Double totalDebit;
    private Double totalCredit;
    private List<BalanceLineDto> lignes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceLineDto {
        private String numeroCompte;
        private String libelleCompte;
        private Double soldePrecedent;
        private Double mouvementDebit;
        private Double mouvementCredit;
        private Double soldeActuel;
    }
}