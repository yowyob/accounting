package com.yowyob.erp.accounting.infrastructure.web.dto;

import java.util.List;

/**
 * Résultat d'un import de plan comptable depuis un fichier XLSX ou CSV.
 */
public record ImportResult(
        int imported,
        int updated,
        int skipped,
        List<String> errors) {
}
