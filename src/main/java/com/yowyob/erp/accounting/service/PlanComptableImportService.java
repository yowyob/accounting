package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.ImportResult;
import com.yowyob.erp.accounting.entity.PlanComptable;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import com.yowyob.erp.config.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsable de l'import d'un plan comptable depuis un fichier XLSX ou
 * CSV.
 * Format attendu : colonnes no_compte, libelle, notes (optionnel)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanComptableImportService {

    private final PlanComptableRepository planComptableRepository;
    private final RedisService redisService;

    private static final String CACHE_PREFIX_ALL = "plancomptable:all:";
    private static final String CACHE_PREFIX_ACTIVE = "plancomptable:active:";
    private static final String CACHE_PREFIX_CLASS = "plancomptable:class:";
    private static final String CACHE_PREFIX_PFX = "plancomptable:prefix:";

    /**
     * Parse et importe un plan comptable depuis un tableau d'octets.
     *
     * @param fileBytes   contenu du fichier
     * @param filename    nom du fichier (pour détecter le type)
     * @param orgId       organisation cible
     * @param currentUser utilisateur courant (pour audit)
     */
    @Transactional
    public Mono<ImportResult> importFromBytes(byte[] fileBytes, String filename,
            UUID orgId, String currentUser) {
        List<RawRow> rows;
        List<String> parseErrors = new ArrayList<>();

        try {
            if (filename != null && filename.toLowerCase().endsWith(".xlsx")) {
                rows = parseXlsx(fileBytes, parseErrors);
            } else {
                rows = parseCsv(fileBytes, parseErrors);
            }
        } catch (Exception e) {
            log.error("Failed to parse file {}: {}", filename, e.getMessage());
            return Mono.just(new ImportResult(0, 0, 0, List.of("Erreur de lecture du fichier : " + e.getMessage())));
        }

        if (rows.isEmpty()) {
            parseErrors.add("Aucune ligne valide trouvée dans le fichier.");
            return Mono.just(new ImportResult(0, 0, 0, parseErrors));
        }

        // counters
        int[] imported = { 0 };
        int[] updated = { 0 };
        int[] skipped = { 0 };

        return Flux.fromIterable(rows)
                .flatMap(row -> planComptableRepository
                        .existsByOrganization_IdAndNo_compte(orgId, row.noCompte())
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                // UPDATE libelle / notes
                                return planComptableRepository
                                        .findByOrganization_IdAndNo_compte(orgId, row.noCompte())
                                        .flatMap(account -> {
                                            account.setLibelle(row.libelle());
                                            if (row.notes() != null)
                                                account.setNotes(row.notes());
                                            account.setUpdated_at(LocalDateTime.now());
                                            account.setUpdated_by(currentUser);
                                            account.setNotNew();
                                            return planComptableRepository.save(account)
                                                    .doOnNext(a -> updated[0]++);
                                        });
                            } else {
                                // INSERT
                                int classe = 0;
                                try {
                                    classe = Character.getNumericValue(row.noCompte().charAt(0));
                                } catch (Exception ignored) {
                                }

                                PlanComptable account = PlanComptable.builder()
                                        .organizationId(orgId)
                                        .no_compte(row.noCompte())
                                        .classe(classe)
                                        .libelle(row.libelle())
                                        .notes(row.notes())
                                        .actif(true)
                                        .created_at(LocalDateTime.now())
                                        .updated_at(LocalDateTime.now())
                                        .created_by(currentUser)
                                        .updated_by(currentUser)
                                        .build();

                                return planComptableRepository.save(account)
                                        .doOnNext(a -> imported[0]++);
                            }
                        })
                        .onErrorResume(e -> {
                            log.warn("Skipped row {}: {}", row.noCompte(), e.getMessage());
                            parseErrors.add("Ignoré " + row.noCompte() + " : " + e.getMessage());
                            skipped[0]++;
                            return Mono.empty();
                        }))
                .then(invalidateCaches(orgId))
                .thenReturn(new ImportResult(imported[0], updated[0], skipped[0], parseErrors));
    }

    // -----------------------------------------------------------------------
    // Parsers
    // -----------------------------------------------------------------------

    private List<RawRow> parseXlsx(byte[] bytes, List<String> errors) throws Exception {
        List<RawRow> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            // Find header row (first row)
            Row header = sheet.getRow(0);
            if (header == null)
                return rows;

            Map<String, Integer> colIndex = buildColumnIndex(header);
            int noCompteIdx = colIndex.getOrDefault("no_compte",
                    colIndex.getOrDefault("nocompte", colIndex.getOrDefault("numero", -1)));
            int libelleIdx = colIndex.getOrDefault("libelle", -1);
            int notesIdx = colIndex.getOrDefault("notes", -1);

            if (noCompteIdx < 0 || libelleIdx < 0) {
                errors.add("Colonnes requises manquantes. Attendu : no_compte, libelle");
                return rows;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;
                String noCompte = cellToString(row.getCell(noCompteIdx)).trim();
                String libelle = cellToString(row.getCell(libelleIdx)).trim();
                String notes = notesIdx >= 0 ? cellToString(row.getCell(notesIdx)).trim() : null;
                if (noCompte.isEmpty() || libelle.isEmpty())
                    continue;
                rows.add(new RawRow(noCompte, libelle, notes.isEmpty() ? null : notes));
            }
        }
        return rows;
    }

    private List<RawRow> parseCsv(byte[] bytes, List<String> errors) throws Exception {
        List<RawRow> rows = new ArrayList<>();
        // Detect delimiter (';' or ',')
        String preview = new String(bytes, 0, Math.min(bytes.length, 500), StandardCharsets.UTF_8);
        char delimiter = preview.contains(";") ? ';' : ',';

        Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setDelimiter(delimiter)
                .setIgnoreEmptyLines(true)
                .build();

        try (CSVParser parser = new CSVParser(reader, format)) {
            Map<String, Integer> headers = parser.getHeaderMap();
            String noCompteCol = resolveColumn(headers, "no_compte", "nocompte", "numero");
            String libelleCol = resolveColumn(headers, "libelle");
            String notesCol = resolveColumn(headers, "notes");

            if (noCompteCol == null || libelleCol == null) {
                errors.add("Colonnes requises manquantes. Attendu : no_compte, libelle");
                return rows;
            }

            for (CSVRecord record : parser) {
                String noCompte = record.get(noCompteCol).trim();
                String libelle = record.get(libelleCol).trim();
                String notes = notesCol != null ? record.get(notesCol).trim() : null;
                if (noCompte.isEmpty() || libelle.isEmpty())
                    continue;
                rows.add(new RawRow(noCompte, libelle, (notes != null && notes.isEmpty()) ? null : notes));
            }
        }
        return rows;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Map<String, Integer> buildColumnIndex(Row header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell != null) {
                map.put(cellToString(cell).trim().toLowerCase().replace(" ", "_"), i);
            }
        }
        return map;
    }

    private String cellToString(Cell cell) {
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf((long) cell.getNumericCellValue())
                    : cell.getStringCellValue();
            default -> cell.getStringCellValue();
        };
    }

    private String resolveColumn(Map<String, Integer> headers, String... candidates) {
        for (String candidate : candidates) {
            for (String key : headers.keySet()) {
                if (key.trim().toLowerCase().replace(" ", "_").equals(candidate))
                    return key;
            }
        }
        return null;
    }

    private Mono<Void> invalidateCaches(UUID orgId) {
        return redisService.delete(CACHE_PREFIX_ALL + orgId)
                .then(redisService.delete(CACHE_PREFIX_ACTIVE + orgId))
                .then();
    }

    /** Ligne brute extraite du fichier */
    private record RawRow(String noCompte, String libelle, String notes) {
    }
}
