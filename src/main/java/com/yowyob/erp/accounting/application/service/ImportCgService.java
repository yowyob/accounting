package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.*;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.*;
import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureAnalytiqueDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.ImportCgRequestDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.ImportCgResultDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportCgService {

    private final DatabaseClient databaseClient;
    private final EcritureAnalytiqueRepository ecritureRepo;
    private final LigneImputationRepository ligneRepo;
    private final JournalAnalytiqueRepository journalRepo;
    private final AxeAnalytiqueRepository axeRepo;
    private final CompteAnalytiqueRepository compteAnalytiqueRepo;
    private final PeriodeAnalytiqueRepository periodeRepo;
    private final EcritureAnalytiqueService ecritureAnalytiqueService;
    private final RegleIncorporationService regleIncorporationService;

    @Transactional
    public Mono<ImportCgResultDto> importFromCg(ImportCgRequestDto request) {
        ImportCgRequestDto req = request != null ? request : ImportCgRequestDto.builder().build();
        boolean force = Boolean.TRUE.equals(req.getForce());

        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(ctx -> {
                UUID orgId = ctx.getT1();
                String user = ctx.getT2();

                return Mono.zip(
                    resolveDateRange(orgId, req),
                    resolveJournalCharges(orgId),
                    resolveDefaultCentre(orgId),
                    loadComptesAnalytiques(orgId),
                    countExistingImports(orgId)
                ).flatMap(tuple -> {
                    DateRange range = tuple.getT1();
                    JournalAnalytique journal = tuple.getT2();
                    AxeAnalytique centre = tuple.getT3();
                    List<CompteAnalytique> comptesAnalytiques = tuple.getT4();
                    int seqBase = tuple.getT5();

                    return loadPeriodes(orgId, req)
                        .collectList()
                        .flatMap(periodes -> fetchChargeLines(orgId, range)
                            .collectList()
                            .flatMap(lines -> processLines(
                                orgId, user, lines, journal, centre, comptesAnalytiques,
                                periodes, force, seqBase)));
                });
            });
    }

    private Mono<ImportCgResultDto> processLines(
            UUID orgId,
            String user,
            List<ChargeLineRow> lines,
            JournalAnalytique journal,
            AxeAnalytique centre,
            List<CompteAnalytique> comptesAnalytiques,
            List<PeriodeAnalytique> periodes,
            boolean force,
            int seqBase) {

        if (lines.isEmpty()) {
            return Mono.just(ImportCgResultDto.builder()
                .created(List.of())
                .ignored(0)
                .errors(List.of())
                .build());
        }

        return Flux.fromIterable(lines)
            .index()
            .concatMap(tuple -> processLine(
                orgId, user, tuple.getT2(), journal, centre, comptesAnalytiques, periodes, force,
                seqBase + tuple.getT1().intValue() + 1))
            .collectList()
            .map(results -> {
                List<EcritureAnalytiqueDto> created = new ArrayList<>();
                List<String> errors = new ArrayList<>();
                int ignored = 0;
                for (LineResult result : results) {
                    switch (result.status()) {
                        case CREATED -> created.add(result.dto());
                        case IGNORED -> ignored++;
                        case ERROR -> errors.add(result.message());
                    }
                }
                return ImportCgResultDto.builder()
                    .created(created)
                    .ignored(ignored)
                    .errors(errors)
                    .build();
            });
    }

    private Mono<LineResult> processLine(
            UUID orgId,
            String user,
            ChargeLineRow line,
            JournalAnalytique journal,
            AxeAnalytique centre,
            List<CompteAnalytique> comptesAnalytiques,
            List<PeriodeAnalytique> periodes,
            boolean force,
            int seq) {

        return regleIncorporationService.isIncorporable(orgId, line.noCompte())
            .flatMap(incorporable -> {
                if (!Boolean.TRUE.equals(incorporable)) {
                    return Mono.just(LineResult.ignored());
                }

        BigDecimal montant = ImportCgHelper.debitAmount(line.montantDebit());
        String libelle = line.detailLibelle() != null && !line.detailLibelle().isBlank()
            ? line.detailLibelle()
            : line.ecritureLibelle();

        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.just(LineResult.ignored());
        }

        Mono<Boolean> duplicateCheck = force
            ? Mono.just(false)
            : ecritureRepo.existsImportDuplicate(orgId, line.ecritureId(), montant, libelle);

        return duplicateCheck.flatMap(isDuplicate -> {
            if (Boolean.TRUE.equals(isDuplicate)) {
                return Mono.just(LineResult.ignored());
            }

            UUID periodeId = resolvePeriodeId(periodes, line.dateEcriture());
            if (periodeId == null) {
                return Mono.just(LineResult.error(
                    "Aucune période analytique pour l'écriture CG du " + line.dateEcriture()
                        + " (compte " + line.noCompte() + ")."));
            }

            UUID natureChargeId = resolveNatureChargeId(comptesAnalytiques, line.noCompte());
            LocalDate dateEffet = line.dateEcriture() != null ? line.dateEcriture() : LocalDate.now();
            int year = dateEffet.getYear();
            String numeroPiece = ImportCgHelper.generateNumeroPiece(year, seq);

            EcritureAnalytique entity = EcritureAnalytique.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .journalId(journal.getId())
                .periodeId(periodeId)
                .numeroPiece(numeroPiece)
                .libelle(libelle)
                .dateEffet(dateEffet)
                .origine("IMPORT_CG")
                .statut("BROUILLON")
                .ecriturecgRef(line.ecritureId())
                .montantTotal(montant)
                .natureChargeId(natureChargeId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(user)
                .updatedBy(user)
                .build();

            LigneImputation ligne = LigneImputation.builder()
                .id(UUID.randomUUID())
                .ecritureId(entity.getId())
                .centreId(centre.getId())
                .montant(montant)
                .sens("DEBIT")
                .libelle(libelle)
                .build();

            return ecritureRepo.save(entity)
                .flatMap(saved -> ligneRepo.save(ligne).thenReturn(saved))
                .flatMap(ecritureAnalytiqueService::enrichForImport)
                .map(LineResult::created)
                .onErrorResume(ex -> {
                    log.warn("Import CG — échec ligne {} : {}", line.detailId(), ex.getMessage());
                    return Mono.just(LineResult.error(
                        "Échec import compte " + line.noCompte() + " : " + ex.getMessage()));
                });
        });
            });
    }

    private Flux<ChargeLineRow> fetchChargeLines(UUID orgId, DateRange range) {
        String sql = """
            SELECT de.id AS detail_id,
                   de.ecriture_id,
                   de.libelle AS detail_libelle,
                   de.montant_debit,
                   c.no_compte,
                   ec.libelle AS ecriture_libelle,
                   ec.date_ecriture
            FROM details_ecritures de
            JOIN comptes c ON de.compte_id = c.id
            JOIN ecritures_comptables ec ON de.ecriture_id = ec.id
            WHERE de.organization_id = :orgId
              AND c.classe = 6
              AND COALESCE(ec.validee, false) = true
              AND COALESCE(de.montant_debit, 0) > 0
              AND ec.date_ecriture BETWEEN :startDate AND :endDate
            ORDER BY ec.date_ecriture, de.id
            """;

        return databaseClient.sql(sql)
            .bind("orgId", orgId)
            .bind("startDate", range.start())
            .bind("endDate", range.end())
            .map((row, metadata) -> new ChargeLineRow(
                row.get("detail_id", UUID.class),
                row.get("ecriture_id", UUID.class),
                row.get("detail_libelle", String.class),
                row.get("montant_debit", BigDecimal.class),
                row.get("no_compte", String.class),
                row.get("ecriture_libelle", String.class),
                row.get("date_ecriture", LocalDate.class)
            ))
            .all();
    }

    private Mono<DateRange> resolveDateRange(UUID orgId, ImportCgRequestDto request) {
        if (request.getPeriodeId() != null) {
            return periodeRepo.findById(request.getPeriodeId())
                .filter(p -> orgId.equals(p.getOrganizationId()))
                .map(p -> new DateRange(p.getDateDebut(), p.getDateFin()))
                .switchIfEmpty(Mono.error(new BusinessException("Période analytique introuvable.")));
        }
        if (request.getExerciceId() != null) {
            return periodeRepo.findByOrganizationIdAndExerciceId(orgId, request.getExerciceId())
                .collectList()
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.error(new BusinessException("Aucune période pour cet exercice."));
                    }
                    LocalDate start = list.stream().map(PeriodeAnalytique::getDateDebut).min(LocalDate::compareTo).orElse(LocalDate.now().withDayOfYear(1));
                    LocalDate end = list.stream().map(PeriodeAnalytique::getDateFin).max(LocalDate::compareTo).orElse(LocalDate.now());
                    return Mono.just(new DateRange(start, end));
                });
        }
        int year = LocalDate.now().getYear();
        return Mono.just(new DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
    }

    private Flux<PeriodeAnalytique> loadPeriodes(UUID orgId, ImportCgRequestDto request) {
        if (request.getPeriodeId() != null) {
            return periodeRepo.findById(request.getPeriodeId())
                .filter(p -> orgId.equals(p.getOrganizationId()))
                .flux();
        }
        if (request.getExerciceId() != null) {
            return periodeRepo.findByOrganizationIdAndExerciceId(orgId, request.getExerciceId());
        }
        return periodeRepo.findByOrganizationId(orgId);
    }

    private Mono<JournalAnalytique> resolveJournalCharges(UUID orgId) {
        return journalRepo.findByOrganizationIdAndActif(orgId, true)
            .filter(j -> "CHARGES".equalsIgnoreCase(j.getType()))
            .next()
            .switchIfEmpty(journalRepo.findByOrganizationIdAndActif(orgId, true).next())
            .switchIfEmpty(Mono.error(new BusinessException(
                "Aucun journal analytique de charges actif. Créez un journal de type CHARGES.")));
    }

    private Mono<AxeAnalytique> resolveDefaultCentre(UUID orgId) {
        return axeRepo.findByOrganizationIdAndActif(orgId, true)
            .filter(a -> "CENTRE_COUT".equalsIgnoreCase(a.getType()))
            .next()
            .switchIfEmpty(axeRepo.findByOrganizationIdAndActif(orgId, true).next())
            .switchIfEmpty(Mono.error(new BusinessException(
                "Aucun centre de coût actif. Créez au moins un axe de type CENTRE_COUT.")));
    }

    private Mono<List<CompteAnalytique>> loadComptesAnalytiques(UUID orgId) {
        return compteAnalytiqueRepo.findByOrganizationIdAndActif(orgId, true).collectList();
    }

    private Mono<Integer> countExistingImports(UUID orgId) {
        return ecritureRepo.findByOrganizationId(orgId)
            .filter(e -> "IMPORT_CG".equals(e.getOrigine()))
            .count()
            .map(Long::intValue)
            .defaultIfEmpty(0);
    }

    private UUID resolvePeriodeId(List<PeriodeAnalytique> periodes, LocalDate date) {
        if (date == null || periodes.isEmpty()) {
            return periodes.isEmpty() ? null : periodes.get(0).getId();
        }
        return periodes.stream()
            .filter(p -> !date.isBefore(p.getDateDebut()) && !date.isAfter(p.getDateFin()))
            .map(PeriodeAnalytique::getId)
            .findFirst()
            .orElse(periodes.get(0).getId());
    }

    private UUID resolveNatureChargeId(List<CompteAnalytique> comptes, String noCompte) {
        if (noCompte == null) {
            return comptes.isEmpty() ? null : comptes.get(0).getId();
        }
        Optional<CompteAnalytique> exact = comptes.stream()
            .filter(c -> noCompte.equals(c.getCode()))
            .findFirst();
        if (exact.isPresent()) {
            return exact.get().getId();
        }
        Optional<CompteAnalytique> prefix = comptes.stream()
            .filter(c -> c.getCode() != null && noCompte.startsWith(c.getCode()))
            .max(Comparator.comparingInt(c -> c.getCode().length()));
        if (prefix.isPresent()) {
            return prefix.get().getId();
        }
        return comptes.isEmpty() ? null : comptes.get(0).getId();
    }

    private record DateRange(LocalDate start, LocalDate end) {}

    private record ChargeLineRow(
        UUID detailId,
        UUID ecritureId,
        String detailLibelle,
        BigDecimal montantDebit,
        String noCompte,
        String ecritureLibelle,
        LocalDate dateEcriture
    ) {}

    private record LineResult(Status status, EcritureAnalytiqueDto dto, String message) {
        enum Status { CREATED, IGNORED, ERROR }

        static LineResult created(EcritureAnalytiqueDto dto) {
            return new LineResult(Status.CREATED, dto, null);
        }

        static LineResult ignored() {
            return new LineResult(Status.IGNORED, null, null);
        }

        static LineResult error(String message) {
            return new LineResult(Status.ERROR, null, message);
        }
    }
}
