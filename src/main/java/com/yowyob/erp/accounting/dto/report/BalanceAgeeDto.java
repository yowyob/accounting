package com.yowyob.erp.accounting.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceAgeeDto {

    private LocalDate dateReference;
    private List<LigneBalanceAgeeDto> clients;
    private List<LigneBalanceAgeeDto> fournisseurs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneBalanceAgeeDto {
        private String noCompte;
        private String libelle;
        private BigDecimal soldeTotal;
        /** 0 à 30 jours */
        private BigDecimal tranche0_30;
        /** 31 à 60 jours */
        private BigDecimal tranche31_60;
        /** 61 à 90 jours */
        private BigDecimal tranche61_90;
        /** Plus de 90 jours */
        private BigDecimal tranches90Plus;
    }
}
