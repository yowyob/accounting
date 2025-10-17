package com.yowyob.erp.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parse le texte d’une facture et en déduit les champs structurés. */
class InvoiceTextParser {

    /**
     * Record holding the structured data extracted from the invoice text.
     * Note: journalComptableId, periodeComptableId, and clientId must often be resolved
     * by a service later, based on the extracted date and business context.
     */
    record ParsedInvoice(
            UUID id,
            BigDecimal montantHT,   // Use BigDecimal for financial precision
            Double tauxTVA,         // ex. 0.18 (as percentage / 100)
            LocalDate date,
            String libelle,
            UUID journalComptableId, 
            UUID periodeComptableId, 
            UUID clientId,            
            Boolean isAchat
    ) {}

    /**
     * Attempts to parse key fields from the raw invoice text.
     *
     * @param text The raw text extracted from the document (e.g., via OCR).
     * @return A ParsedInvoice record with extracted and calculated fields.
     */
    ParsedInvoice parse(String text) {
        String t = normalize(text);

        // --- Montant HT (using BigDecimal) ---
        BigDecimal montantHT = findBigDecimal(t, "(?i)montant\\s*ht\\s*[:\\-]??\\s*([0-9\\s.,]+)");
        if (montantHT == null) montantHT = findBigDecimal(t, "(?i)ht\\s*[:\\-]??\\s*([0-9\\s.,]+)");

        // --- Taux TVA (using Double for calculation) ---
        Double tvaPct = findDouble(t, "(?i)tva\\s*[:\\-]??\\s*([0-9\\s.,]+)\\s*%"); // ex: 18 %
        if (tvaPct == null) {
            // sometimes labeled "TVA (18%)"
            tvaPct = findDouble(t, "(?i)tva\\s*\\(?\\s*([0-9\\s.,]+)\\s*%\\s*\\)?");
        }
        Double tauxTVA = tvaPct != null ? (tvaPct / 100d) : 0.18;

        // --- Date ---
        LocalDate date = findDate(t, "(?i)date\\s*[:\\-]??\\s*([0-9]{2}/[0-9]{2}/[0-9]{4})");
        if (date == null) {
            date = findDate(t, "(?i)le\\s*([0-9]{2}/[0-9]{2}/[0-9]{4})");
        }

        // --- Libelle/Description ---
        String libelle = findString(t, "(?i)(objet|description|prestation)\\s*[:\\-]??\\s*(.+)");
        if (libelle != null && libelle.length() > 160) libelle = libelle.substring(0, 160);

        // --- Heuristique achat vs vente ---
        boolean isAchat = t.contains("facture fournisseur") || t.contains("achat") || t.contains("fournisseur");

        return new ParsedInvoice(
                null,
                montantHT,
                tauxTVA,
                date,
                libelle,
                // These IDs must be resolved by the calling service, hence 'null' for now.
                null,
                null,
                null,
                isAchat
        );
    }

    /* ---------------- utils parsing ---------------- */

    private String normalize(String s) {
        return s == null ? "" : s.replace('\u00A0', ' ').replaceAll("[\\t\\r]+", " ").trim();
    }

    /**
     * Finds a numeric value in the text using regex and converts it to Double.
     * Used mainly for intermediate calculations like VAT percentage.
     */
    private Double findDouble(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) {
            String raw = m.group(1)
                    .replace(" ", "")
                    .replace("\u00A0", "")
                    .replace(".", "")
                    .replace(",", "."); // Handle comma as decimal separator
            try { return Double.parseDouble(raw); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * Finds a numeric value and converts it to BigDecimal for financial precision.
     */
    private BigDecimal findBigDecimal(String text, String regex) {
        Double d = findDouble(text, regex);
        return d != null ? BigDecimal.valueOf(d) : null;
    }


    private String findString(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        // Group 1 or 2 is used depending on the complexity of the regex (e.g., if group 1 is the label and group 2 is the content)
        if (m.find()) {
            // Try to return the last group, which usually holds the content after the label
            for (int i = m.groupCount(); i >= 1; i--) {
                if (m.group(i) != null) {
                    return m.group(i).trim();
                }
            }
        }
        return null;
    }

    private LocalDate findDate(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) {
            String raw = m.group(1);
            try {
                return LocalDate.parse(raw, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
