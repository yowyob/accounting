package com.yowyob.erp.accounting.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

/**
 * Parses invoice text and extracts structured fields.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
class InvoiceTextParser {

    /**
     * Record holding the structured data extracted from the invoice text.
     * Note: journal_comptable_id, periode_comptable_id, and client_id must often be
     * resolved
     * by a service later, based on the extracted date and business context.
     */
    record ParsedInvoice(
            UUID id,
            BigDecimal montant_ht, // Use BigDecimal for financial precision
            Double taux_tva, // ex. 0.18 (as percentage / 100)
            LocalDate date,
            String libelle,
            UUID journal_comptable_id,
            UUID periode_comptable_id,
            UUID client_id,
            Boolean is_achat) {
    }

    /**
     * Attempts to parse key fields from the raw invoice text.
     *
     * @param text The raw text extracted from the document (e.g., via OCR).
     * @return A ParsedInvoice record with extracted and calculated fields.
     */
    ParsedInvoice parse(String text) {
        String t = normalize(text);

        // --- Montant HT (using BigDecimal) ---
        BigDecimal montant_ht = findMoney(t,
                "(?i)(?:subtotal|sous\\s*total|total)\\s*ht\\s*[:\\-]?\\s*([0-9][0-9\\s.,]*)\\s*(?:xaf|fcfa)?");
        if (montant_ht == null)
            montant_ht = findMoney(t, "(?i)montant\\s*ht\\s*[:\\-]??\\s*([0-9][0-9\\s.,]*)");
        if (montant_ht == null)
            montant_ht = findMoney(t, "(?i)ht\\s*[:\\-]??\\s*([0-9][0-9\\s.,]*)\\s*(?:xaf|fcfa)?");

        // --- Taux TVA (using Double for calculation) ---
        Double tva_pct = findDouble(t, "(?i)tva\\s*[:\\-]??\\s*([0-9\\s.,]+)\\s*%"); // ex: 18 %
        if (tva_pct == null) {
            // sometimes labeled "TVA (18%)"
            tva_pct = findDouble(t, "(?i)tva\\s*\\(?\\s*([0-9\\s.,]+)\\s*%\\s*\\)?");
        }
        Double taux_tva = tva_pct != null ? (tva_pct / 100d) : 0.18;

        // --- Date ---
        LocalDate date = findDate(t, "(?i)date\\s*[:\\-]??\\s*([0-9]{2}/[0-9]{2}/[0-9]{4})");
        if (date == null) {
            date = findDate(t, "(?i)le\\s*([0-9]{2}/[0-9]{2}/[0-9]{4})");
        }
        if (date == null) {
            date = findDate(t, "(?i)(?:issue\\s*date|date\\s*d['’]?emission)\\s*[:\\-]?\\s*([0-9]{1,2}\\s+[A-Za-z]{3,9}\\s+[0-9]{4})",
                    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH));
        }

        // --- Libelle/Description ---
        String libelle = findString(t, "(?i)(objet|description|prestation)\\s*[:\\-]??\\s*(.+)");
        if (libelle != null && libelle.length() > 160)
            libelle = libelle.substring(0, 160);

        // --- Heuristic purchase vs sale ---
        boolean is_achat = t.contains("facture fournisseur") || t.contains("achat") || t.contains("fournisseur");

        return new ParsedInvoice(
                null,
                montant_ht,
                taux_tva,
                date,
                libelle,
                // These IDs must be resolved by the calling service, hence 'null' for now.
                null,
                null,
                null,
                is_achat);
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
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
            }
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

    private BigDecimal findMoney(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (!m.find()) {
            return null;
        }
        return parseMoney(m.group(1));
    }

    private BigDecimal parseMoney(String raw) {
        if (raw == null) {
            return null;
        }

        String value = raw
                .replace("\u00A0", "")
                .replace(" ", "")
                .trim();

        if (value.isEmpty()) {
            return null;
        }

        int lastComma = value.lastIndexOf(',');
        int lastDot = value.lastIndexOf('.');

        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                value = value.replace(".", "").replace(",", ".");
            } else {
                value = value.replace(",", "");
            }
        } else if (lastComma >= 0) {
            int digitsAfter = value.length() - lastComma - 1;
            value = digitsAfter == 3 ? value.replace(",", "") : value.replace(",", ".");
        } else if (lastDot >= 0) {
            int digitsAfter = value.length() - lastDot - 1;
            if (digitsAfter == 3) {
                value = value.replace(".", "");
            }
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String findString(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        // Group 1 or 2 is used depending on the complexity of the regex (e.g., if group
        // 1 is the label and group 2 is the content)
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
        return findDate(text, regex, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private LocalDate findDate(String text, String regex, DateTimeFormatter formatter) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) {
            String raw = m.group(1);
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
