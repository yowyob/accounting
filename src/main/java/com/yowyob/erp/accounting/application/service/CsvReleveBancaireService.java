package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.ReleveBancaire;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

/**
 * Service for parsing CSV bank statements reactively.
 */
@Service
public class CsvReleveBancaireService {

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
                    .toFormatter(Locale.FRENCH));

    /**
     * Parses a bank statement from a reactive multipart FilePart.
     *
     * @param file the uploaded FilePart
     * @return a Mono containing the list of parsed bank operations
     */
    public Mono<List<ReleveBancaire>> parseReleveBancaire(FilePart file) {
        return DataBufferUtils.join(file.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> Mono.fromCallable(() -> parseCsvBytes(bytes)));
    }

    private List<ReleveBancaire> parseCsvBytes(byte[] bytes) throws Exception {
        List<ReleveBancaire> operations = new ArrayList<>();
        UUID organizationId = null;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] cols = line.split(";|,|\t");
                if (cols.length < 4)
                    continue;

                ReleveBancaire op = new ReleveBancaire();
                op.setOrganizationId(organizationId);
                op.setNew(true);

                int idxDate    = detectColumnIndex(cols, "date", "operation", "valeur");
                int idxLibelle = detectColumnIndex(cols, "libell", "description", "motif", "intitul");
                int idxMontant = detectColumnIndex(cols, "montant", "credit", "debit", "amount");

                op.setDateOperation(parseDate(cols[idxDate]).atStartOfDay());
                op.setLibelle(normalizeLibelle(cols[idxLibelle]));

                BigDecimal montant = parseMontant(cols, idxMontant);
                op.setMontant(montant.abs());
                op.setSens(montant.compareTo(BigDecimal.ZERO) >= 0 ? "C" : "D");
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
                if (header.contains(kw))
                    return i;
            }
        }
        return Math.min(0, cols.length - 1);
    }

    private LocalDate parseDate(String text) {
        String clean = text.trim().replace("\"", "");
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(clean, fmt);
            } catch (Exception ignored) {
            }
        }
        return LocalDate.now();
    }

    private BigDecimal parseMontant(String[] cols, int idxMontant) {
        try {
            String val = cols[idxMontant].replaceAll("[^\\d,.-]", "").replace(",", ".");
            return new BigDecimal(val);
        } catch (Exception e) {
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
        return lib.replaceAll("\"", "").replaceAll(" +", " ").trim().toUpperCase();
    }

    private String detecterCategorie(String libelle) {
        if (libelle.contains("VIR") || libelle.contains("VIREMENT"))   return "VIREMENT";
        if (libelle.contains("CHQ") || libelle.contains("CHEQUE"))     return "CHEQUE";
        if (libelle.contains("FRAIS") || libelle.contains("COMMISSION") || libelle.contains("AGIOS"))
            return "FRAIS BANCAIRES";
        if (libelle.contains("RETRAIT") || libelle.contains("GAB"))    return "RETRAIT";
        if (libelle.contains("CARTE")  || libelle.contains("CB"))      return "CARTE";
        return "AUTRE";
    }

    /**
     * Parses a CSV bank statement and returns a list of transaction maps.
     */
    public Mono<List<Map<String, Object>>> parseReleveCsv(FilePart file, String compte_bancaire) {
        return parseReleveBancaire(file).map(operations -> {
            List<Map<String, Object>> result = new ArrayList<>();
            for (ReleveBancaire op : operations) {
                result.add(Map.of(
                        "date",      op.getDateOperation(),
                        "libelle",   op.getLibelle(),
                        "montant",   op.getMontant(),
                        "sens",      op.getSens(),
                        "categorie", op.getCategorieDetectee()));
            }
            return result;
        });
    }

    public Mono<List<Map<String, Object>>> getListeReleves(UUID organization_id) {
        return Mono.just(new ArrayList<>());
    }

    public Mono<Map<String, Object>> importerReleveEnEcritures(UUID releve_id, String user) {
        return Mono.just(Map.of(
                "releve_id",        releve_id,
                "ecritures_creees", 0,
                "message",          "Import non implémenté"));
    }
}
