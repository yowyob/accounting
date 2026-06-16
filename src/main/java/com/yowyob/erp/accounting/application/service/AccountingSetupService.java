package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.ExerciceComptable;
import com.yowyob.erp.accounting.domain.model.JournalComptable;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.domain.model.PeriodeComptable;
import com.yowyob.erp.accounting.domain.port.in.PlanComptableUseCase;
import com.yowyob.erp.accounting.infrastructure.initialization.OperationComptableInitializationService;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.ExerciceComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.OperationComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PeriodeComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PlanComptableRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.AccountingSetupRequest;
import com.yowyob.erp.accounting.infrastructure.web.dto.AccountingSetupResponse;
import com.yowyob.erp.accounting.infrastructure.web.dto.AccountingSetupResponse.StepResult;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.domain.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Self-service accounting onboarding: provisions, on demand and idempotently, the building blocks a
 * new organization needs (chart of accounts, journals, fiscal year, periods, operation templates).
 *
 * <p>Triggered by an ADMIN / RESPONSABLE_COMPTABLE from the frontend wizard rather than seeded
 * automatically at boot for a single hard-coded organization. Every step checks for existing data
 * first, so the wizard can be re-run safely, and a failure in one step is reported without aborting
 * the others.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountingSetupService {

    private final PlanComptableUseCase planComptable;
    private final OperationComptableInitializationService operationProvisioning;
    private final JournalComptableRepository journalRepository;
    private final ExerciceComptableRepository exerciceRepository;
    private final PeriodeComptableRepository periodeRepository;
    private final PlanComptableRepository planComptableRepository;
    private final OperationComptableRepository operationRepository;

    /** Standard OHADA journals: {code, libellé, type}. */
    private static final List<String[]> STANDARD_JOURNALS = List.of(
            new String[] { "AN", "Journal des Achats", AppConstants.JournalTypes.PURCHASES },
            new String[] { "VE", "Journal des Ventes", AppConstants.JournalTypes.SALES },
            new String[] { "CA", "Journal de Caisse", AppConstants.JournalTypes.CASH },
            new String[] { "BQ", "Journal de Banque", AppConstants.JournalTypes.BANK },
            new String[] { "OD", "Journal des Opérations Diverses", AppConstants.JournalTypes.GENERAL });

    // ---------------------------------------------------------------------------------------------
    // Orchestration
    // ---------------------------------------------------------------------------------------------

    public Mono<AccountingSetupResponse> runSetup(UUID organizationId, AccountingSetupRequest request) {
        int year = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
        log.info("Running accounting setup for organization {} (year={}, request={})",
                organizationId, year, request);

        return ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system")
                .flatMap(user -> {
                    List<Mono<StepResult>> stepMonos = new ArrayList<>();
                    if (request.isPlanComptable()) {
                        stepMonos.add(guard("planComptable", "Plan comptable OHADA", provisionPlan(organizationId)));
                    }
                    if (request.isJournaux()) {
                        stepMonos.add(guard("journaux", "Journaux comptables", provisionJournaux(organizationId, user)));
                    }
                    if (request.isExercice()) {
                        stepMonos.add(guard("exercice", "Exercice " + year, provisionExercice(organizationId, year, user)));
                    }
                    if (request.isPeriodes()) {
                        stepMonos.add(guard("periodes", "Périodes mensuelles " + year,
                                provisionPeriodes(organizationId, year, user)));
                    }
                    if (request.isOperations()) {
                        stepMonos.add(guard("operations", "Comptes essentiels & opérations standard",
                                provisionOperations(organizationId)));
                    }
                    return Flux.concat(stepMonos).collectList()
                            .map(steps -> AccountingSetupResponse.builder()
                                    .organizationId(organizationId)
                                    .year(year)
                                    .steps(steps)
                                    .build());
                });
    }

    /** Read-only snapshot of what is already provisioned, so the wizard can pre-check boxes. */
    public Mono<AccountingSetupResponse> status(UUID organizationId, Integer requestedYear) {
        int year = requestedYear != null ? requestedYear : LocalDate.now().getYear();
        String exerciceCode = String.valueOf(year);

        Mono<StepResult> plan = planComptableRepository.findByOrganization_Id(organizationId).count()
                .map(n -> step("planComptable", "Plan comptable OHADA", n > 0 ? "ALREADY_PRESENT" : "MISSING",
                        n + " compte(s)"));
        Mono<StepResult> journaux = journalRepository.findByOrganization_Id(organizationId).count()
                .map(n -> step("journaux", "Journaux comptables", n > 0 ? "ALREADY_PRESENT" : "MISSING",
                        n + " journal(aux)"));
        Mono<StepResult> exercice = exerciceRepository.findByOrganizationIdAndCode(organizationId, exerciceCode)
                .map(e -> step("exercice", "Exercice " + exerciceCode, "ALREADY_PRESENT", "Exercice présent"))
                .defaultIfEmpty(step("exercice", "Exercice " + exerciceCode, "MISSING", "Aucun exercice " + exerciceCode));
        Mono<StepResult> periodes = exerciceRepository.findByOrganizationIdAndCode(organizationId, exerciceCode)
                .flatMap(e -> periodeRepository.findByExerciceId(e.getId()).count())
                .defaultIfEmpty(0L)
                .map(n -> step("periodes", "Périodes mensuelles " + year, n > 0 ? "ALREADY_PRESENT" : "MISSING",
                        n + "/12 période(s)"));
        Mono<StepResult> operations = operationRepository.findByOrganization_Id(organizationId).count()
                .map(n -> step("operations", "Comptes essentiels & opérations standard",
                        n > 0 ? "ALREADY_PRESENT" : "MISSING", n + " opération(s)"));

        return Flux.concat(plan, journaux, exercice, periodes, operations).collectList()
                .map(steps -> AccountingSetupResponse.builder()
                        .organizationId(organizationId)
                        .year(year)
                        .steps(steps)
                        .build());
    }

    // ---------------------------------------------------------------------------------------------
    // Steps (each idempotent)
    // ---------------------------------------------------------------------------------------------

    private Mono<StepResult> provisionPlan(UUID organizationId) {
        return planComptable.provisionPlanComptableForOrganization(organizationId)
                .map(created -> step("planComptable", "Plan comptable OHADA",
                        created > 0 ? "CREATED" : "ALREADY_PRESENT",
                        created > 0 ? created + " compte(s) créé(s)" : "Plan déjà présent"));
    }

    private Mono<StepResult> provisionJournaux(UUID organizationId, String user) {
        return Flux.fromIterable(STANDARD_JOURNALS)
                .concatMap(j -> journalRepository
                        .existsByOrganization_IdAndCode_journal(organizationId, j[0])
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.just(0)
                                : journalRepository.save(buildJournal(organizationId, j[0], j[1], j[2], user))
                                        .thenReturn(1)))
                .reduce(0, Integer::sum)
                .map(created -> step("journaux", "Journaux comptables",
                        created > 0 ? "CREATED" : "ALREADY_PRESENT",
                        created > 0 ? created + " journal(aux) créé(s)" : "5 journaux déjà présents"));
    }

    private Mono<StepResult> provisionExercice(UUID organizationId, int year, String user) {
        String code = String.valueOf(year);
        return exerciceRepository.findByOrganizationIdAndCode(organizationId, code)
                .map(e -> step("exercice", "Exercice " + code, "ALREADY_PRESENT", "Exercice " + code + " déjà présent"))
                .switchIfEmpty(Mono.defer(() -> exerciceRepository.save(buildExercice(organizationId, year, user))
                        .thenReturn(step("exercice", "Exercice " + code, "CREATED",
                                "Exercice " + code + " créé (01/01 → 31/12)"))));
    }

    private Mono<StepResult> provisionPeriodes(UUID organizationId, int year, String user) {
        // Periods require a fiscal year — ensure it exists first (idempotent).
        return ensureExercice(organizationId, year, user)
                .flatMap(exercice -> Flux.range(1, 12)
                        .concatMap(month -> {
                            String code = String.format("%d-%02d", year, month);
                            return periodeRepository.findByOrganization_IdAndCode(organizationId, code)
                                    .map(p -> 0)
                                    .switchIfEmpty(Mono.defer(() -> periodeRepository
                                            .save(buildPeriode(organizationId, exercice.getId(), year, month, user))
                                            .thenReturn(1)));
                        })
                        .reduce(0, Integer::sum)
                        .map(created -> step("periodes", "Périodes mensuelles " + year,
                                created > 0 ? "CREATED" : "ALREADY_PRESENT",
                                created > 0 ? created + " période(s) mensuelle(s) créée(s)" : "12 périodes déjà présentes")));
    }

    private Mono<StepResult> provisionOperations(UUID organizationId) {
        return operationProvisioning.provisionForOrganization(organizationId)
                .thenReturn(step("operations", "Comptes essentiels & opérations standard", "CREATED",
                        "Comptes essentiels + opérations VENTE/ACHAT/PAIEMENT provisionnés (idempotent)"));
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private Mono<ExerciceComptable> ensureExercice(UUID organizationId, int year, String user) {
        String code = String.valueOf(year);
        return exerciceRepository.findByOrganizationIdAndCode(organizationId, code)
                .switchIfEmpty(Mono.defer(() -> exerciceRepository.save(buildExercice(organizationId, year, user))));
    }

    private JournalComptable buildJournal(UUID organizationId, String code, String libelle, String type, String user) {
        log.info("Creating journal {} - {} for organization {}", code, libelle, organizationId);
        return JournalComptable.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .organization(new Organization(organizationId))
                .code_journal(code)
                .libelle(libelle)
                .type_journal(type)
                .actif(true)
                .created_at(LocalDateTime.now())
                .updated_at(LocalDateTime.now())
                .created_by(user)
                .updated_by(user)
                .build();
    }

    private ExerciceComptable buildExercice(UUID organizationId, int year, String user) {
        log.info("Creating fiscal year {} for organization {}", year, organizationId);
        return ExerciceComptable.builder()
                .organizationId(organizationId)
                .code(String.valueOf(year))
                .libelle("Exercice " + year)
                .date_debut(LocalDate.of(year, 1, 1))
                .date_fin(LocalDate.of(year, 12, 31))
                .cloture(false)
                .created_at(LocalDateTime.now())
                .updated_at(LocalDateTime.now())
                .created_by(user)
                .updated_by(user)
                .build();
    }

    private PeriodeComptable buildPeriode(UUID organizationId, UUID exerciceId, int year, int month, String user) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return PeriodeComptable.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .exerciceId(exerciceId)
                .code(String.format("%d-%02d", year, month))
                .date_debut(start)
                .date_fin(end)
                .cloturee(false)
                .created_at(LocalDateTime.now())
                .updated_at(LocalDateTime.now())
                .created_by(user)
                .updated_by(user)
                .build();
    }

    private static StepResult step(String key, String label, String status, String detail) {
        return StepResult.builder().key(key).label(label).status(status).detail(detail).build();
    }

    /** Wraps a step so a failure is reported as an ERROR result instead of aborting the wizard. */
    private Mono<StepResult> guard(String key, String label, Mono<StepResult> step) {
        return step.onErrorResume(e -> {
            log.error("Accounting setup step '{}' failed: {}", key, e.getMessage(), e);
            return Mono.just(step(key, label, "ERROR", e.getMessage()));
        });
    }
}
