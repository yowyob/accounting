package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.BudgetDto;
import com.yowyob.erp.accounting.dto.BudgetVsRealiseDto;
import com.yowyob.erp.accounting.entity.Budget;
import com.yowyob.erp.accounting.entity.BudgetLigneCompte;
import com.yowyob.erp.accounting.repository.BudgetRepository;
import com.yowyob.erp.accounting.repository.BudgetLigneCompteRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budget_repository;
    private final BudgetLigneCompteRepository budgetLigneCompteRepository;
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
                    .parentId(dto.getParentId())
                    .compteId(dto.getCompteId())
                    .code(dto.getCode())
                    .nom(dto.getNom())
                    .montantAlloue(dto.getMontantAlloue())
                    .libelle(dto.getLibelle())
                    .notes(dto.getNotes())
                    .type(dto.getType() != null ? dto.getType() : "EXERCICE")
                    .statut(dto.getStatut() != null ? dto.getStatut() : "BROUILLON")
                    .seuilAlerte(dto.getSeuilAlerte() != null ? dto.getSeuilAlerte() : 80)
                    .dateDebut(dto.getDateDebut())
                    .dateFin(dto.getDateFin())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy(user)
                    .updatedBy(user)
                    .build();

                // Validation de plafond par rapport au parent
                Mono<Void> validateLimit = validateBudgetLimit(entity);

                return validateLimit.then(budget_repository.save(entity))
                    .flatMap(saved -> {
                        Mono<Void> saveAxes = Mono.empty();
                        if (dto.getAxeIds() != null && !dto.getAxeIds().isEmpty()) {
                            saveAxes = Flux.fromIterable(dto.getAxeIds())
                                .flatMap(axeId -> budget_repository.linkAxe(saved.getId(), axeId))
                                .then();
                        }

                        Mono<Void> saveLines = Mono.empty();
                        if ("ANALYTIQUE".equals(saved.getType()) && dto.getCompteLines() != null && !dto.getCompteLines().isEmpty()) {
                            saveLines = Flux.fromIterable(dto.getCompteLines())
                                .flatMap(lineDto -> {
                                    BudgetLigneCompte line = BudgetLigneCompte.builder()
                                        .id(UUID.randomUUID())
                                        .budgetId(saved.getId())
                                        .compteId(lineDto.getCompteId())
                                        .montantAlloue(lineDto.getMontantAlloue())
                                        .description(lineDto.getDescription())
                                        .build();
                                    return budgetLigneCompteRepository.save(line);
                                })
                                .then();
                        }

                        return saveAxes.then(saveLines).then(enrichDto(saved, orgId));
                    });
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
                        existing.setNom(dto.getNom());
                        existing.setMontantAlloue(dto.getMontantAlloue());
                        existing.setLibelle(dto.getLibelle());
                        existing.setNotes(dto.getNotes());
                        existing.setStatut(dto.getStatut() != null ? dto.getStatut() : existing.getStatut());
                        existing.setSeuilAlerte(dto.getSeuilAlerte() != null ? dto.getSeuilAlerte() : existing.getSeuilAlerte());
                        existing.setDateDebut(dto.getDateDebut() != null ? dto.getDateDebut() : existing.getDateDebut());
                        existing.setDateFin(dto.getDateFin() != null ? dto.getDateFin() : existing.getDateFin());
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();

                        Mono<Void> validateLimit = validateBudgetLimit(existing);

                        return validateLimit.then(budget_repository.save(existing))
                            .flatMap(saved -> {
                                Mono<Void> updateAxes = budget_repository.unlinkAllAxes(saved.getId())
                                    .then(Mono.defer(() -> {
                                        if (dto.getAxeIds() == null || dto.getAxeIds().isEmpty()) {
                                            return Mono.empty();
                                        }
                                        return Flux.fromIterable(dto.getAxeIds())
                                            .flatMap(axeId -> budget_repository.linkAxe(saved.getId(), axeId))
                                            .then();
                                    }));

                                Mono<Void> updateLines = budgetLigneCompteRepository.deleteByBudgetId(saved.getId())
                                    .then(Mono.defer(() -> {
                                        if (!"ANALYTIQUE".equals(saved.getType()) || dto.getCompteLines() == null || dto.getCompteLines().isEmpty()) {
                                            return Mono.empty();
                                        }
                                        return Flux.fromIterable(dto.getCompteLines())
                                            .flatMap(lineDto -> {
                                                BudgetLigneCompte line = BudgetLigneCompte.builder()
                                                    .id(UUID.randomUUID())
                                                    .budgetId(saved.getId())
                                                    .compteId(lineDto.getCompteId())
                                                    .montantAlloue(lineDto.getMontantAlloue())
                                                    .description(lineDto.getDescription())
                                                    .build();
                                                return budgetLigneCompteRepository.save(line);
                                            })
                                            .then();
                                    }));

                                return updateAxes.then(updateLines).then(enrichDto(saved, orgId));
                            });
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> budget_repository.findById(id)
                .filter(b -> orgId.equals(b.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Budget", id.toString())))
                .flatMap(budget -> budget_repository.unlinkAllAxes(budget.getId())
                    .then(budgetLigneCompteRepository.deleteByBudgetId(budget.getId()))
                    .then(budget_repository.delete(budget))));
    }

    public Mono<BudgetDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> budget_repository.findById(id)
                .filter(b -> orgId.equals(b.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Budget", id.toString())))
                .flatMap(b -> enrichDto(b, orgId)));
    }

    public Flux<BudgetDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> budget_repository.findByOrganizationId(orgId)
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
    // WORKFLOW ACTION
    // ─────────────────────────────────────────────

    @Transactional
    public Mono<BudgetDto> validate(UUID id) {
        return updateStatut(id, "VALIDE");
    }

    @Transactional
    public Mono<BudgetDto> activate(UUID id) {
        return updateStatut(id, "ACTIF");
    }

    @Transactional
    public Mono<BudgetDto> deactivate(UUID id) {
        return updateStatut(id, "INACTIF");
    }

    private Mono<BudgetDto> updateStatut(UUID id, String status) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                return budget_repository.findById(id)
                    .filter(b -> orgId.equals(b.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Budget", id.toString())))
                    .flatMap(b -> {
                        b.setStatut(status);
                        b.setUpdatedAt(LocalDateTime.now());
                        b.setUpdatedBy(user);
                        b.setNotNew();
                        return budget_repository.save(b).flatMap(saved -> enrichDto(saved, orgId));
                    });
            });
    }

    // ─────────────────────────────────────────────
    // COMPARAISON BUDGET VS RÉALISÉ
    // ─────────────────────────────────────────────

    public Mono<BudgetVsRealiseDto> getBudgetVsRealise(UUID exerciceId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> exercice_repository.findById(exerciceId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("ExerciceComptable", exerciceId.toString())))
                .flatMap(exercice -> {
                    // Pour garder l'ancienne signature compatible, on récupère les budgets analytiques ou de type PREVISIONNEL
                    Mono<List<Budget>> budgetsMono = budget_repository
                        .findByOrganizationIdAndExerciceId(orgId, exerciceId)
                        .collectList();

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

                            return compte_repository.findAllByOrganization_Id(orgId)
                                .collectMap(c -> c.getId(), c -> c)
                                .map(compteMap -> {
                                    List<BudgetVsRealiseDto.LigneBudgetVsRealiseDto> lignes = new ArrayList<>();
                                    
                                    for (Budget b : budgets) {
                                        if ("ANALYTIQUE".equals(b.getType())) {
                                            // Pour les budgets analytiques, on peut agréger par compte
                                            // Nous aurons des lignes dans LigneBudgetVsRealiseDto pour ce budget
                                            BigDecimal budgetVal = b.getMontantAlloue() != null ? b.getMontantAlloue() : BigDecimal.ZERO;
                                            BigDecimal totalRealise = BigDecimal.ZERO;
                                            
                                            // Remplir une ligne par budget
                                            lignes.add(BudgetVsRealiseDto.LigneBudgetVsRealiseDto.builder()
                                                .noCompte(b.getCode() != null ? b.getCode() : "ANA")
                                                .libelleCompte(b.getNom())
                                                .montantBudget(budgetVal)
                                                .montantRealise(BigDecimal.ZERO) // sera calculé via details
                                                .ecart(budgetVal.negate())
                                                .tauxRealisation(BigDecimal.ZERO)
                                                .build());
                                        } else if (b.getCompteId() != null) {
                                            var compte = compteMap.get(b.getCompteId());
                                            String noCompte = compte != null ? compte.getNo_compte() : "?";
                                            String libelleC = compte != null ? compte.getLibelle() : "?";
                                            BigDecimal budgetVal = b.getMontantAlloue() != null ? b.getMontantAlloue() : BigDecimal.ZERO;
                                            BigDecimal realise = realiseMap.getOrDefault(b.getCompteId(), BigDecimal.ZERO).abs();
                                            BigDecimal ecart = realise.subtract(budgetVal);
                                            BigDecimal taux = budgetVal.compareTo(BigDecimal.ZERO) != 0
                                                ? realise.divide(budgetVal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                                                : BigDecimal.ZERO;

                                            lignes.add(BudgetVsRealiseDto.LigneBudgetVsRealiseDto.builder()
                                                .noCompte(noCompte)
                                                .libelleCompte(libelleC)
                                                .montantBudget(budgetVal)
                                                .montantRealise(realise)
                                                .ecart(ecart)
                                                .tauxRealisation(taux)
                                                .build());
                                        }
                                    }

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
    // UTILITAIRES DE VALIDATION ET ENRICHISSEMENT
    // ─────────────────────────────────────────────

    private Mono<Void> validateBudgetLimit(Budget entity) {
        if (entity.getParentId() == null || entity.getMontantAlloue() == null || "BROUILLON".equals(entity.getStatut())) {
            return Mono.empty();
        }
        
        return budget_repository.findById(entity.getParentId())
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Budget Parent", entity.getParentId().toString())))
            .flatMap(parent -> {
                BigDecimal parentMax = parent.getMontantAlloue();
                if (parentMax == null) {
                    return Mono.empty();
                }
                
                return budget_repository.findByOrganizationIdAndParentId(entity.getOrganizationId(), parent.getId())
                    .filter(sibling -> !sibling.getId().equals(entity.getId())) // exclure soi-même en cas d'update
                    .map(sibling -> sibling.getMontantAlloue() != null ? sibling.getMontantAlloue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .flatMap(sumOfSiblings -> {
                        BigDecimal newTotal = sumOfSiblings.add(entity.getMontantAlloue());
                        if (newTotal.compareTo(parentMax) > 0) {
                            return Mono.error(new BusinessException("Le montant total alloué (" + newTotal 
                                + ") dépasse l'enveloppe allouée du budget parent (" + parentMax + ")"));
                        }
                        return Mono.empty();
                    });
            });
    }

    private Mono<BudgetDto> enrichDto(Budget b, UUID orgId) {
        // Chargement du nom du parent si présent
        Mono<String> parentNomMono = Mono.just("");
        if (b.getParentId() != null) {
            parentNomMono = budget_repository.findById(b.getParentId())
                .map(Budget::getNom)
                .defaultIfEmpty("");
        }

        // Chargement des axes
        Mono<List<UUID>> axeIdsMono = budget_repository.findLinkedAxeIds(b.getId()).collectList();

        // Chargement des lignes de comptes avec leurs réalisés respectifs
        Mono<List<BudgetDto.LigneBudgetCompteDto>> linesMono = Mono.just(new ArrayList<>());
        if ("ANALYTIQUE".equals(b.getType())) {
            linesMono = budgetLigneCompteRepository.findByBudgetId(b.getId())
                .flatMap(line -> compte_repository.findById(line.getCompteId())
                    .flatMap(compte -> {
                        // Calcul du réalisé de cette ligne sur la plage de dates
                        LocalDate start = b.getDateDebut();
                        LocalDate end = b.getDateFin();
                        
                        return getConsumedAmountForAccounts(orgId, List.of(line.getCompteId()), start, end)
                            .map(consumed -> BudgetDto.LigneBudgetCompteDto.builder()
                                .id(line.getId())
                                .compteId(line.getCompteId())
                                .noCompte(compte.getNo_compte())
                                .libelleCompte(compte.getLibelle())
                                .montantAlloue(line.getMontantAlloue())
                                .montantConsomme(consumed)
                                .description(line.getDescription())
                                .build());
                    }))
                .collectList();
        }

        // Pour les budgets EXERCICE ou PERIODE, calcul global du réalisé
        Mono<BigDecimal> totalConsumedMono = Mono.just(BigDecimal.ZERO);
        if ("ANALYTIQUE".equals(b.getType())) {
            totalConsumedMono = linesMono.map(lines -> lines.stream()
                .map(BudgetDto.LigneBudgetCompteDto::getMontantConsomme)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        } else if (b.getDateDebut() != null && b.getDateFin() != null) {
            // Pour exercice ou période, récupérer tous les comptes budgétés en enfants récursifs
            totalConsumedMono = getCompteIdsForBudget(orgId, b.getId())
                .collectList()
                .flatMap(compteIds -> getConsumedAmountForAccounts(orgId, compteIds, b.getDateDebut(), b.getDateFin()));
        }

        return Mono.zip(parentNomMono, axeIdsMono, linesMono, totalConsumedMono)
            .flatMap(tuple -> {
                String parentNom = tuple.getT1();
                List<UUID> axeIds = tuple.getT2();
                List<BudgetDto.LigneBudgetCompteDto> lines = tuple.getT3();
                BigDecimal totalConsumed = tuple.getT4();

                BudgetDto.BudgetDtoBuilder builder = BudgetDto.builder()
                    .id(b.getId())
                    .exerciceId(b.getExerciceId())
                    .periodeId(b.getPeriodeId())
                    .parentId(b.getParentId())
                    .parentNom(parentNom.isEmpty() ? null : parentNom)
                    .code(b.getCode())
                    .nom(b.getNom())
                    .montantAlloue(b.getMontantAlloue())
                    .montantConsomme(totalConsumed)
                    .libelle(b.getLibelle())
                    .notes(b.getNotes())
                    .type(b.getType())
                    .statut(b.getStatut())
                    .seuilAlerte(b.getSeuilAlerte())
                    .dateDebut(b.getDateDebut())
                    .dateFin(b.getDateFin())
                    .axeIds(axeIds.isEmpty() ? null : axeIds)
                    .compteLines(lines.isEmpty() ? null : lines)
                    .createdAt(b.getCreatedAt())
                    .createdBy(b.getCreatedBy());

                if (b.getCompteId() != null) {
                    return compte_repository.findById(b.getCompteId())
                        .map(compte -> {
                            builder.compteId(b.getCompteId());
                            builder.noCompte(compte.getNo_compte());
                            builder.libelleCompte(compte.getLibelle());
                            return builder.build();
                        })
                        .defaultIfEmpty(builder.build());
                }

                return Mono.just(builder.build());
            });
    }

    private Flux<UUID> getCompteIdsForBudget(UUID orgId, UUID budgetId) {
        // Récupérer récursivement les comptes liés aux budgets enfants analytiques
        return budget_repository.findByOrganizationIdAndParentId(orgId, budgetId)
            .flatMap(child -> {
                if ("ANALYTIQUE".equals(child.getType())) {
                    return budgetLigneCompteRepository.findByBudgetId(child.getId())
                        .map(BudgetLigneCompte::getCompteId);
                } else {
                    return getCompteIdsForBudget(orgId, child.getId());
                }
            });
    }

    private Mono<BigDecimal> getConsumedAmountForAccounts(UUID orgId, List<UUID> compteIds, LocalDate start, LocalDate end) {
        if (compteIds == null || compteIds.isEmpty() || start == null || end == null) {
            return Mono.just(BigDecimal.ZERO);
        }
        LocalDateTime startLdt = start.atStartOfDay();
        LocalDateTime endLdt = end.plusDays(1).atStartOfDay();

        return detail_repository.findByOrganization_IdAndDateRange(orgId, startLdt, endLdt)
            .filter(d -> compteIds.contains(d.getCompte_id()))
            .map(d -> {
                BigDecimal deb = d.getMontant_debit() != null ? d.getMontant_debit() : BigDecimal.ZERO;
                BigDecimal cre = d.getMontant_credit() != null ? d.getMontant_credit() : BigDecimal.ZERO;
                return deb.subtract(cre).abs();
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
