package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.BudgetDto;
import com.yowyob.erp.accounting.dto.BudgetVsRealiseDto;
import com.yowyob.erp.accounting.entity.Budget;
import com.yowyob.erp.accounting.repository.BudgetRepository;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.ExerciceComptableRepository;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestion des budgets et comparaison budget vs réalisé.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budget_repository;
    private final CompteRepository compte_repository;
    private final DetailEcritureRepository detail_repository;
    private final ExerciceComptableRepository exercice_repository;

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    @Transactional
    public Mono<BudgetDto> create(BudgetDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                Budget entity = Budget.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .exerciceId(dto.getExerciceId())
                    .periodeId(dto.getPeriodeId())
                    .compteId(dto.getCompteId())
                    .montantBudget(dto.getMontantBudget())
                    .libelle(dto.getLibelle())
                    .notes(dto.getNotes())
                    .type(dto.getType() != null ? dto.getType() : "PREVISIONNEL")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy(user)
                    .updatedBy(user)
                    .build();
                return budget_repository.save(entity)
                    .flatMap(saved -> enrichDto(saved, orgId));
            });
    }

    @Transactional
    public Mono<BudgetDto> update(UUID id, BudgetDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                return budget_repository.findById(id)
                    .filter(b -> orgId.equals(b.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Budget", id.toString())))
                    .flatMap(existing -> {
                        existing.setMontantBudget(dto.getMontantBudget());
                        existing.setLibelle(dto.getLibelle());
                        existing.setNotes(dto.getNotes());
                        existing.setType(dto.getType() != null ? dto.getType() : existing.getType());
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();
                        return budget_repository.save(existing).flatMap(saved -> enrichDto(saved, orgId));
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> budget_repository.findById(id)
                .filter(b -> orgId.equals(b.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Budget", id.toString())))
                .flatMap(budget_repository::delete));
    }

    public Mono<BudgetDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> budget_repository.findById(id)
                .filter(b -> orgId.equals(b.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Budget", id.toString())))
                .flatMap(b -> enrichDto(b, orgId)));
    }

    public Flux<BudgetDto> findByExercice(UUID exerciceId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> budget_repository.findByOrganizationIdAndExerciceId(orgId, exerciceId)
                .flatMap(b -> enrichDto(b, orgId)));
    }

    public Flux<BudgetDto> findByPeriode(UUID periodeId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> budget_repository.findByOrganizationIdAndPeriodeId(orgId, periodeId)
                .flatMap(b -> enrichDto(b, orgId)));
    }

    // ─────────────────────────────────────────────
    // COMPARAISON BUDGET VS RÉALISÉ
    // ─────────────────────────────────────────────

    /**
     * Compare le budget prévisionnel avec les montants réellement comptabilisés
     * sur la période de l'exercice donné.
     */
    public Mono<BudgetVsRealiseDto> getBudgetVsRealise(UUID exerciceId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> exercice_repository.findById(exerciceId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("ExerciceComptable", exerciceId.toString())))
                .flatMap(exercice -> {
                    // 1. Récupère toutes les lignes budgétaires de l'exercice
                    Mono<List<Budget>> budgetsMono = budget_repository
                        .findByOrganizationIdAndExerciceId(orgId, exerciceId)
                        .collectList();

                    // 2. Récupère tous les détails d'écritures de l'exercice
                    Mono<Map<UUID, BigDecimal>> realiseByCompteMono = detail_repository
                        .findByOrganization_IdAndDateRange(orgId,
                            exercice.getDate_debut().atStartOfDay(),
                            exercice.getDate_fin().plusDays(1).atStartOfDay())
                        .collectList()
                        .map(details -> details.stream()
                            .collect(Collectors.groupingBy(
                                d -> d.getCompte_id(),
                                Collectors.reducing(BigDecimal.ZERO,
                                    d -> {
                                        BigDecimal deb = d.getMontant_debit() != null ? d.getMontant_debit() : BigDecimal.ZERO;
                                        BigDecimal cre = d.getMontant_credit() != null ? d.getMontant_credit() : BigDecimal.ZERO;
                                        return deb.subtract(cre);
                                    },
                                    BigDecimal::add))));

                    return Mono.zip(budgetsMono, realiseByCompteMono)
                        .flatMap(tuple -> {
                            List<Budget> budgets = tuple.getT1();
                            Map<UUID, BigDecimal> realiseMap = tuple.getT2();

                            // Enrichissement avec les informations des comptes
                            return compte_repository.findAllByOrganization_Id(orgId)
                                .collectMap(c -> c.getId(), c -> c)
                                .map(compteMap -> {
                                    List<BudgetVsRealiseDto.LigneBudgetVsRealiseDto> lignes = budgets.stream()
                                        .map(b -> {
                                            var compte = compteMap.get(b.getCompteId());
                                            String noCompte = compte != null ? compte.getNo_compte() : "?";
                                            String libelleC = compte != null ? compte.getLibelle() : "?";
                                            BigDecimal budget = b.getMontantBudget();
                                            BigDecimal realise = realiseMap.getOrDefault(b.getCompteId(), BigDecimal.ZERO).abs();
                                            BigDecimal ecart = realise.subtract(budget);
                                            BigDecimal taux = budget.compareTo(BigDecimal.ZERO) != 0
                                                ? realise.divide(budget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                                                : BigDecimal.ZERO;

                                            return BudgetVsRealiseDto.LigneBudgetVsRealiseDto.builder()
                                                .noCompte(noCompte)
                                                .libelleCompte(libelleC)
                                                .montantBudget(budget)
                                                .montantRealise(realise)
                                                .ecart(ecart)
                                                .tauxRealisation(taux)
                                                .build();
                                        })
                                        .collect(Collectors.toList());

                                    BigDecimal totalBudget = lignes.stream()
                                        .map(BudgetVsRealiseDto.LigneBudgetVsRealiseDto::getMontantBudget)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    BigDecimal totalRealise = lignes.stream()
                                        .map(BudgetVsRealiseDto.LigneBudgetVsRealiseDto::getMontantRealise)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                    return BudgetVsRealiseDto.builder()
                                        .exerciceId(exerciceId)
                                        .exerciceCode(exercice.getCode())
                                        .lignes(lignes)
                                        .totalBudget(totalBudget)
                                        .totalRealise(totalRealise)
                                        .totalEcart(totalRealise.subtract(totalBudget))
                                        .build();
                                });
                        });
                }));
    }

    // ─────────────────────────────────────────────
    // UTILITAIRE
    // ─────────────────────────────────────────────

    private Mono<BudgetDto> enrichDto(Budget b, UUID orgId) {
        return compte_repository.findById(b.getCompteId())
            .map(compte -> BudgetDto.builder()
                .id(b.getId())
                .exerciceId(b.getExerciceId())
                .periodeId(b.getPeriodeId())
                .compteId(b.getCompteId())
                .noCompte(compte.getNo_compte())
                .libelleCompte(compte.getLibelle())
                .montantBudget(b.getMontantBudget())
                .libelle(b.getLibelle())
                .notes(b.getNotes())
                .type(b.getType())
                .createdAt(b.getCreatedAt())
                .createdBy(b.getCreatedBy())
                .build())
            .defaultIfEmpty(BudgetDto.builder()
                .id(b.getId())
                .exerciceId(b.getExerciceId())
                .periodeId(b.getPeriodeId())
                .compteId(b.getCompteId())
                .montantBudget(b.getMontantBudget())
                .libelle(b.getLibelle())
                .type(b.getType())
                .build());
    }
}
