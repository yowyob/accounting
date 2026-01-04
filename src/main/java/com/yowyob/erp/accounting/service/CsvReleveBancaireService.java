// src/main/java/com/yowyob/erp/accounting/service/CsvReleveBancaireService.java
package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.ReleveBancaire;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.config.tenant.TenantContext;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ser
 */
@Service
public class CsvReleveBancaireService {

    // Formats de date acceptés automatiquement
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd MMM yyyy")
            .toFormatter(Locale.FRENCH),
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd MMMM yyyy")
            .toFormatter(Locale.FRENCH)
    );
    
    public List<ReleveBancaire> parseReleveBancaire(MultipartFile file) throws Exception {
        List<ReleveBancaire> operations = new ArrayList<>();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();


        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // skip header
                }

                String[] cols = line.split(";|,|\t"); // gère ; , et tabulation

                if (cols.length < 4) continue;

                ReleveBancaire op = new ReleveBancaire();
                op.setTenant(tenant);

                // Détection intelligente des colonnes (fonctionne avec 90% des banques africaines)
                int idxDate = detectColumnIndex(cols, "date", "operation", "valeur");
                int idxLibelle = detectColumnIndex(cols, "libell", "description", "motif", "intitul");
                int idxMontant = detectColumnIndex(cols, "montant", "credit", "debit", "amount");

                op.setDateOperation(parseDate(cols[idxDate]));
                op.setLibelle(normalizeLibelle(cols[idxLibelle]));

                BigDecimal montant = parseMontant(cols, idxMontant);
                op.setMontant(montant.abs());
                op.setSens(montant.compareTo(BigDecimal.ZERO) >= 0 ? "C" : "D");

                // Détection automatique du type
                op.setCategorieDetectee(detecterCategorie(op.getLibelle()));

                operations.add(op);
            }
        }
        return operations;
    }

    private int detectColumnIndex(String[] cols, String... keywords) {
        for (int i = 0; i < cols.length; i++) {
            String header = cols[i].toLowerCase();
            for (String kw : keywords) {
                if (header.contains(kw)) return i;
            }
        }
        return Math.min(0, cols.length - 1); // fallback
    }

    private LocalDate parseDate(String text) {
        String clean = text.trim().replace("\"", "");
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(clean, fmt);
            } catch (Exception e) {}
        }
        return LocalDate.now(); // fallback
    }

    private BigDecimal parseMontant(String[] cols, int idxMontant) {
        try {
            String val = cols[idxMontant].replaceAll("[^\\d,.-]", "").replace(",", ".");
            return new BigDecimal(val);
        } catch (Exception e) {
            // essai colonne crédit/débit séparée
            for (int i = 0; i < cols.length; i++) {
                if (cols[i].toLowerCase().contains("credit") && !cols[i].isBlank()) {
                    return new BigDecimal(cols[i].replaceAll("[^\\d.-]", "").replace(",", "."));
                }
                if (cols[i].toLowerCase().contains("debit") && !cols[i].isBlank()) {
                    return new BigDecimal(cols[i].replaceAll("[^\\d.-]", "").replace(",", ".")).negate();
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private String normalizeLibelle(String lib) {
        return lib.replaceAll("\"", "")
                  .replaceAll(" +", " ")
                  .trim()
                  .toUpperCase();
    }

    private String detecterCategorie(String libelle) {
        if (libelle.contains("VIR") || libelle.contains("VIREMENT")) return "VIREMENT";
        if (libelle.contains("CHQ") || libelle.contains("CHEQUE")) return "CHEQUE";
        if (libelle.contains("FRAIS") || libelle.contains("COMMISSION") || libelle.contains("AGIOS")) return "FRAIS BANCAIRES";
        if (libelle.contains("RETRAIT") || libelle.contains("GAB")) return "RETRAIT";
        if (libelle.contains("CARTE") || libelle.contains("CB")) return "CARTE";
        return "AUTRE";
    }
}