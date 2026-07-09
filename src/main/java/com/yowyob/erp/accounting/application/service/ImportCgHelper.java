package com.yowyob.erp.accounting.application.service;

import java.math.BigDecimal;

final class ImportCgHelper {

    private ImportCgHelper() {}

    static boolean isIncorporable(String noCompte) {
        return isIncorporableByDefault(noCompte);
    }

    static boolean isIncorporableByDefault(String noCompte) {
        if (noCompte == null || noCompte.isBlank()) {
            return false;
        }
        String prefix3 = noCompte.length() >= 3 ? noCompte.substring(0, 3) : noCompte;
        return !prefix3.startsWith("66")
            && !prefix3.startsWith("67")
            && !prefix3.startsWith("69");
    }

    static BigDecimal debitAmount(BigDecimal montantDebit) {
        return montantDebit == null ? BigDecimal.ZERO : montantDebit;
    }

    static String generateNumeroPiece(int year, int sequence) {
        return String.format("ECRIT-ANALYTIQUE-%d-%04d", year, sequence);
    }
}
