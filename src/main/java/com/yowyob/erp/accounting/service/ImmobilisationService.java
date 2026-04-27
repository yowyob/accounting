package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.AmortissementLigneDto;
import com.yowyob.erp.accounting.dto.ImmobilisationDto;
import com.yowyob.erp.accounting.entity.AmortissementLigne;
import com.yowyob.erp.accounting.entity.ExerciceComptable;
import com.yowyob.erp.accounting.entity.Immobilisation;
import com.yowyob.erp.accounting.repository.AmortissementLigneRepository;
import com.yowyob.erp.accounting.repository.ExerciceComptableRepository;
import com.yowyob.erp.accounting.repository.ImmobilisationRepository;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des immobilisations et amortissements.
 * Méthodes supportées : LINEAIRE, DEGRESSIF, UNITES_PRODUCTION.
 * Inclut CRUD complet + cession/mise au rebut.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImmobilisationService {

    private final ImmobilisationRepository immo_repository;
    private final AmortissementLigneRepository amort_repository;
    private final ExerciceComptableRepository exercice_repository;
    private final DatabaseClient databaseClient;

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    @Transactional
    public Mono<ImmobilisationDto> create(ImmobilisationDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                Immobilisation entity = toEntity(dto);
                entity.setId(UUID.randomUUID());
                entity.setOrganizationId(orgId);
                entity.setCreatedBy(user);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setStatut("ACTIF");
                return immo_repository.save(entity).map(this::toDto);
            });
    }

    public Flux<ImmobilisationDto> findAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> immo_repository.findByOrganizationId(orgId).map(this::toDto));
    }

    public Flux<ImmobilisationDto> findByStatut(String statut) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> immo_repository.findByOrganizationIdAndStatut(orgId, statut).map(this::toDto));
    }

    public Mono<ImmobilisationDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> immo_repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Immobilisation", id.toString())))
                .map(this::toDto));
    }

    @Transactional
    public Mono<ImmobilisationDto> update(UUID id, ImmobilisationDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                return immo_repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Immobilisation", id.toString())))
                    .flatMap(existing -> {
                        if (!"ACTIF".equals(existing.getStatut())) {
                            return Mono.error(new BusinessException(
                                "Impossible de modifier une immobilisation avec statut : " + existing.getStatut()));
                        }
                        existing.setCode(dto.getCode());
                        existing.setLibelle(dto.getLibelle());
                        existing.setDateAcquisition(dto.getDateAcquisition());
                        existing.setValeurOrigine(dto.getValeurOrigine());
                        existing.setValeurResiduelle(dto.getValeurResiduelle() != null ? dto.getValeurResiduelle() : BigDecimal.ZERO);
                        existing.setDureeVie(dto.getDureeVie());
                        existing.setMethodeAmortissement(dto.getMethodeAmortissement());
                        existing.setCoefficientDegressif(dto.getCoefficientDegressif());
                        existing.setCapaciteTotaleProduction(dto.getCapaciteTotaleProduction());
                        existing.setCompteImmoId(dto.getCompteImmoId());
                        existing.setCompteAmortId(dto.getCompteAmortId());
                        existing.setCompteDotationId(dto.getCompteDotationId());
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();
                        return immo_repository.save(existing).map(this::toDto);
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> immo_repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Immobilisation", id.toString())))
                .flatMap(existing -> {
                    if (!"ACTIF".equals(existing.getStatut())) {
                        return Mono.error(new BusinessException("Seules les immobilisations actives peuvent être supprimées."));
                    }
                    return amort_repository.findByImmoId(id)
                        .filter(AmortissementLigne::isComptabilisee)
                        .hasElements()
                        .flatMap(hasPosted -> {
                            if (hasPosted) {
                                return Mono.error(new BusinessException(
                                    "Impossible de supprimer : des amortissements ont déjà été comptabilisés."));
                            }
                            return amort_repository.findByImmoId(id)
                                .flatMap(amort_repository::delete)
                                .then(immo_repository.delete(existing));
                        });
                }));
    }

    /**
     * Cession d'une immobilisation : génère l'écriture de cession OHADA.
     * Débit 481 (Créances sur cession), Crédit 28x (Amortissements cumulés),
     * Crédit/Débit 82 (Produit/Perte de cession), Crédit 2x (Valeur brute).
     */
    @Transactional
    public Mono<ImmobilisationDto> ceder(UUID id, ImmobilisationDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                return immo_repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Immobilisation", id.toString())))
                    .flatMap(immo -> {
                        if (!"ACTIF".equals(immo.getStatut())) {
                            return Mono.error(new BusinessException("Immobilisation non active."));
                        }
                        if (dto.getDateCession() == null || dto.getPrixCession() == null) {
                            return Mono.error(new BusinessException("Date et prix de cession obligatoires."));
                        }

                        return getCumulAmortissement(id)
                            .flatMap(cumulAmort -> {
                                BigDecimal vnc = immo.getValeurOrigine().subtract(cumulAmort);
                                BigDecimal prix = dto.getPrixCession();
                                BigDecimal resultatCession = prix.subtract(vnc);
                                boolean profit = resultatCession.compareTo(BigDecimal.ZERO) >= 0;

                                String libelle = "Cession immobilisation : " + immo.getLibelle();
                                LocalDate dateCession = dto.getDateCession();

                                return creerEcritureSysteme(orgId, dateCession, libelle, "OD")
                                    .flatMap(ecritureId -> {
                                        // 481 Débit : créance sur cession
                                        // 28x Débit : amortissements cumulés
                                        // 2x  Crédit : valeur brute
                                        // 82  Crédit/Débit : résultat de cession
                                        List<Mono<Void>> ops = new ArrayList<>();
                                        if (dto.getCompteProductCessionId() != null) {
                                            ops.add(insererDetailEcritureDirect(ecritureId, orgId,
                                                dto.getCompteProductCessionId(), prix, "DEBIT"));
                                        }
                                        ops.add(insererDetailEcritureDirect(ecritureId, orgId,
                                            immo.getCompteAmortId(), cumulAmort, "DEBIT"));
                                        ops.add(insererDetailEcritureDirect(ecritureId, orgId,
                                            immo.getCompteImmoId(), immo.getValeurOrigine(), "CREDIT"));
                                        if (dto.getCompteVNCId() != null && !profit) {
                                            ops.add(insererDetailEcritureDirect(ecritureId, orgId,
                                                dto.getCompteVNCId(), resultatCession.abs(), "DEBIT"));
                                        } else if (dto.getCompteVNCId() != null && profit) {
                                            ops.add(insererDetailEcritureDirect(ecritureId, orgId,
                                                dto.getCompteVNCId(), resultatCession, "CREDIT"));
                                        }
                                        return Mono.when(ops);
                                    })
                                    .then(Mono.defer(() -> {
                                        immo.setStatut("CEDE");
                                        immo.setUpdatedAt(LocalDateTime.now());
                                        immo.setUpdatedBy(user);
                                        immo.setNotNew();
                                        return immo_repository.save(immo).map(this::toDto);
                                    }));
                            });
                    });
            });
    }

    /**
     * Mise au rebut d'une immobilisation.
     */
    @Transactional
    public Mono<ImmobilisationDto> mettreAuRebut(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                return immo_repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Immobilisation", id.toString())))
                    .flatMap(immo -> {
                        immo.setStatut("MISE_AU_REBUT");
                        immo.setUpdatedAt(LocalDateTime.now());
                        immo.setUpdatedBy(user);
                        immo.setNotNew();
                        return immo_repository.save(immo).map(this::toDto);
                    });
            });
    }

    public Flux<AmortissementLigneDto> getTableauAmortissement(UUID id) {
        return amort_repository.findByImmoId(id)
            .map(l -> AmortissementLigneDto.builder()
                .id(l.getId())
                .immoId(l.getImmoId())
                .dateEcheance(l.getDateEcheance())
                .baseCalcul(l.getBaseCalcul())
                .taux(l.getTaux())
                .annuite(l.getAnnuite())
                .cumulAmortissement(l.getCumulAmortissement())
                .valeurNetteComptable(l.getValeurNetteComptable())
                .comptabilisee(l.isComptabilisee())
                .ecritureId(l.getEcritureId())
                .build());
    }

    // ─────────────────────────────────────────────
    // GÉNÉRATION DU TABLEAU D'AMORTISSEMENT
    // ─────────────────────────────────────────────

    @Transactional
    public Mono<Void> genererTableauAmortissement(UUID immoId) {
        return immo_repository.findById(immoId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Immobilisation", immoId.toString())))
            .flatMap(immo -> amort_repository.findByImmoId(immoId)
                .filter(AmortissementLigne::isComptabilisee)
                .hasElements()
                .flatMap(hasPosted -> {
                    if (hasPosted) {
                        return Mono.error(new BusinessException(
                            "Impossible de régénérer : des lignes ont déjà été comptabilisées."));
                    }
                    return amort_repository.findByImmoId(immoId)
                        .flatMap(amort_repository::delete)
                        .then(Mono.just(immo));
                }))
            .flatMap(immo -> {
                String methode = immo.getMethodeAmortissement();
                return switch (methode) {
                    case "LINEAIRE" -> genererLineaire(immo);
                    case "DEGRESSIF" -> genererDegressif(immo);
                    case "UNITES_PRODUCTION" -> Mono.error(new BusinessException(
                        "Pour UNITES_PRODUCTION, utilisez genererTableauAmortissementUnitesProduction(id, unitesPrevues[])"));
                    default -> Mono.error(new BusinessException("Méthode inconnue : " + methode));
                };
            });
    }

    @Transactional
    public Mono<Void> genererTableauAmortissementUnitesProduction(UUID immoId, List<BigDecimal> unitesByYear) {
        return immo_repository.findById(immoId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Immobilisation", immoId.toString())))
            .flatMap(immo -> {
                if (!"UNITES_PRODUCTION".equals(immo.getMethodeAmortissement())) {
                    return Mono.error(new BusinessException("L'immobilisation n'utilise pas la méthode UNITES_PRODUCTION."));
                }
                if (immo.getCapaciteTotaleProduction() == null || immo.getCapaciteTotaleProduction().compareTo(BigDecimal.ZERO) <= 0) {
                    return Mono.error(new BusinessException("Capacité totale de production non définie."));
                }
                return amort_repository.findByImmoId(immoId)
                    .flatMap(amort_repository::delete)
                    .then(Mono.just(immo));
            })
            .flatMap(immo -> genererUnitesProduction(immo, unitesByYear));
    }

    // ─────────────────────────────────────────────
    // MÉTHODE LINÉAIRE
    // ─────────────────────────────────────────────

    private Mono<Void> genererLineaire(Immobilisation immo) {
        List<AmortissementLigne> schedule = new ArrayList<>();
        BigDecimal base = immo.getValeurOrigine().subtract(immo.getValeurResiduelle());
        BigDecimal annualRate = BigDecimal.valueOf(100)
            .divide(BigDecimal.valueOf(immo.getDureeVie()), 4, RoundingMode.HALF_UP);
        BigDecimal annualAnnuity = base.multiply(annualRate)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        LocalDate acqDate = immo.getDateAcquisition();
        BigDecimal cumul = BigDecimal.ZERO;

        for (int year = 0; year <= immo.getDureeVie(); year++) {
            LocalDate yearStart = acqDate.withDayOfYear(1).plusYears(year);
            LocalDate yearEnd = yearStart.withMonth(12).withDayOfMonth(31);
            BigDecimal annuity;

            if (year == 0) {
                long days = ChronoUnit.DAYS.between(acqDate, yearEnd) + 1;
                annuity = annualAnnuity.multiply(BigDecimal.valueOf(days))
                    .divide(BigDecimal.valueOf(360), 2, RoundingMode.HALF_UP);
            } else if (year == immo.getDureeVie()) {
                annuity = base.subtract(cumul);
            } else {
                annuity = annualAnnuity;
            }

            if (annuity.compareTo(BigDecimal.ZERO) <= 0) continue;

            cumul = cumul.add(annuity);
            schedule.add(buildLigne(immo.getId(), yearEnd, base, annualRate, annuity, cumul,
                immo.getValeurOrigine().subtract(cumul)));

            if (cumul.compareTo(base) >= 0) break;
        }

        return amort_repository.saveAll(schedule).then();
    }

    // ─────────────────────────────────────────────
    // MÉTHODE DÉGRESSIVE
    // ─────────────────────────────────────────────

    private Mono<Void> genererDegressif(Immobilisation immo) {
        if (immo.getCoefficientDegressif() == null || immo.getCoefficientDegressif().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new BusinessException(
                "Le coefficient dégressif est obligatoire (ex: 1.5, 2.0, 2.5) pour la méthode dégressive."));
        }

        List<AmortissementLigne> schedule = new ArrayList<>();
        BigDecimal base = immo.getValeurOrigine().subtract(immo.getValeurResiduelle());
        BigDecimal tauxLineaire = BigDecimal.ONE
            .divide(BigDecimal.valueOf(immo.getDureeVie()), 6, RoundingMode.HALF_UP);
        BigDecimal tauxDegressif = tauxLineaire.multiply(immo.getCoefficientDegressif());

        LocalDate acqDate = immo.getDateAcquisition();
        BigDecimal vnc = base; // valeur nette comptable restante
        BigDecimal cumul = BigDecimal.ZERO;
        int dureeRestante = immo.getDureeVie();

        for (int year = 0; year < immo.getDureeVie(); year++) {
            LocalDate yearStart = acqDate.withDayOfYear(1).plusYears(year);
            LocalDate yearEnd = yearStart.withMonth(12).withDayOfMonth(31);

            // On bascule en linéaire si le taux linéaire sur la durée restante > taux dégressif
            BigDecimal tauxLineaireRestant = dureeRestante > 0
                ? BigDecimal.ONE.divide(BigDecimal.valueOf(dureeRestante), 6, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

            BigDecimal tauxRetenu = tauxDegressif.compareTo(tauxLineaireRestant) >= 0
                ? tauxDegressif : tauxLineaireRestant;

            BigDecimal annuity;
            if (year == 0) {
                // Prorata temporis première année
                long days = ChronoUnit.DAYS.between(acqDate, yearEnd) + 1;
                BigDecimal prorata = BigDecimal.valueOf(days)
                    .divide(BigDecimal.valueOf(360), 6, RoundingMode.HALF_UP);
                annuity = vnc.multiply(tauxRetenu).multiply(prorata).setScale(2, RoundingMode.HALF_UP);
            } else if (year == immo.getDureeVie() - 1) {
                annuity = vnc; // dernière annuité = VNC restante
            } else {
                annuity = vnc.multiply(tauxRetenu).setScale(2, RoundingMode.HALF_UP);
            }

            if (annuity.compareTo(BigDecimal.ZERO) <= 0) break;

            vnc = vnc.subtract(annuity);
            cumul = cumul.add(annuity);
            dureeRestante--;

            schedule.add(buildLigne(immo.getId(), yearEnd, base, tauxRetenu.multiply(BigDecimal.valueOf(100)), annuity, cumul,
                immo.getValeurOrigine().subtract(cumul)));

            if (vnc.compareTo(immo.getValeurResiduelle()) <= 0) break;
        }

        return amort_repository.saveAll(schedule).then();
    }

    // ─────────────────────────────────────────────
    // MÉTHODE UNITÉS DE PRODUCTION
    // ─────────────────────────────────────────────

    private Mono<Void> genererUnitesProduction(Immobilisation immo, List<BigDecimal> unitesByYear) {
        BigDecimal base = immo.getValeurOrigine().subtract(immo.getValeurResiduelle());
        BigDecimal tauxUnitaire = base.divide(immo.getCapaciteTotaleProduction(), 6, RoundingMode.HALF_UP);

        List<AmortissementLigne> schedule = new ArrayList<>();
        LocalDate acqDate = immo.getDateAcquisition();
        BigDecimal cumul = BigDecimal.ZERO;

        for (int i = 0; i < unitesByYear.size(); i++) {
            BigDecimal unites = unitesByYear.get(i);
            BigDecimal annuity = unites.multiply(tauxUnitaire).setScale(2, RoundingMode.HALF_UP);
            if (annuity.compareTo(BigDecimal.ZERO) <= 0) continue;

            LocalDate yearEnd = acqDate.withDayOfYear(1).plusYears(i).withMonth(12).withDayOfMonth(31);
            cumul = cumul.add(annuity);

            schedule.add(buildLigne(immo.getId(), yearEnd, base, tauxUnitaire.multiply(BigDecimal.valueOf(100)),
                annuity, cumul, immo.getValeurOrigine().subtract(cumul)));

            if (cumul.compareTo(base) >= 0) break;
        }

        return amort_repository.saveAll(schedule).then();
    }

    // ─────────────────────────────────────────────
    // COMPTABILISATION DES DOTATIONS
    // ─────────────────────────────────────────────

    @Transactional
    public Mono<Void> comptabiliserAmortissements(UUID exerciceId) {
        return exercice_repository.findById(exerciceId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("ExerciceComptable", exerciceId.toString())))
            .flatMapMany(ex -> amort_repository.findByExerciceIdAndComptabiliseeFalse(exerciceId)
                .flatMap(ligne -> immo_repository.findById(ligne.getImmoId())
                    .flatMap(immo -> creerEcritureAmortissement(ex, immo, ligne))))
            .then();
    }

    // ─────────────────────────────────────────────
    // UTILITAIRES INTERNES
    // ─────────────────────────────────────────────

    private Mono<BigDecimal> getCumulAmortissement(UUID immoId) {
        return amort_repository.findByImmoId(immoId)
            .filter(AmortissementLigne::isComptabilisee)
            .map(AmortissementLigne::getAnnuite)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AmortissementLigne buildLigne(UUID immoId, LocalDate dateEcheance, BigDecimal base,
            BigDecimal taux, BigDecimal annuity, BigDecimal cumul, BigDecimal vnc) {
        return AmortissementLigne.builder()
            .id(UUID.randomUUID())
            .immoId(immoId)
            .dateEcheance(dateEcheance)
            .baseCalcul(base)
            .taux(taux)
            .annuite(annuity)
            .cumulAmortissement(cumul)
            .valeurNetteComptable(vnc)
            .comptabilisee(false)
            .build();
    }

    private Mono<Void> creerEcritureAmortissement(ExerciceComptable ex, Immobilisation immo, AmortissementLigne ligne) {
        String libelle = "Dotation amortissement " + ex.getCode() + " - " + immo.getLibelle();
        return creerEcritureSysteme(ex.getOrganizationId(), ex.getDate_fin(), libelle, "OD")
            .flatMap(ecritureId -> Mono.when(
                insererDetailEcritureDirect(ecritureId, ex.getOrganizationId(), immo.getCompteDotationId(),
                    ligne.getAnnuite(), "DEBIT"),
                insererDetailEcritureDirect(ecritureId, ex.getOrganizationId(), immo.getCompteAmortId(),
                    ligne.getAnnuite(), "CREDIT"))
                .then(Mono.defer(() -> {
                    ligne.setComptabilisee(true);
                    ligne.setEcritureId(ecritureId);
                    return amort_repository.save(ligne);
                })))
            .then();
    }

    private Mono<UUID> creerEcritureSysteme(UUID organizationId, LocalDate date, String libelle, String codeJournal) {
        String sql = """
            INSERT INTO ecritures_comptables
            (id, organization_id, date_ecriture, libelle, journal_id, validee, created_at, numero_ecriture)
            VALUES (:id, :organizationId, :date, :libelle, NULL, true, NOW(), :numero)
            RETURNING id
            """;
        UUID id = UUID.randomUUID();
        String numero = codeJournal + "-AMORT-" + UUID.randomUUID().toString().substring(0, 8);

        return databaseClient.sql(sql)
            .bind("id", id)
            .bind("organizationId", organizationId)
            .bind("date", date)
            .bind("libelle", libelle)
            .bind("numero", numero)
            .map(row -> row.get("id", UUID.class))
            .one();
    }

    private Mono<Void> insererDetailEcritureDirect(UUID ecritureId, UUID organizationId,
            UUID compteId, BigDecimal montant, String sens) {
        String sql = """
            INSERT INTO details_ecritures
            (id, ecriture_id, organization_id, compte_id, libelle, sens, montant_debit, montant_credit, date_ecriture, created_at)
            VALUES (:id, :ecId, :tId, :cId, 'Dotation aux amortissements', :sens,
                   CASE WHEN :sens = 'DEBIT' THEN :montant ELSE 0 END,
                   CASE WHEN :sens = 'CREDIT' THEN :montant ELSE 0 END,
                   NOW(), NOW())
            """;
        return databaseClient.sql(sql)
            .bind("id", UUID.randomUUID())
            .bind("ecId", ecritureId)
            .bind("tId", organizationId)
            .bind("cId", compteId)
            .bind("sens", sens)
            .bind("montant", montant)
            .then();
    }

    // ─────────────────────────────────────────────
    // MAPPING
    // ─────────────────────────────────────────────

    private ImmobilisationDto toDto(Immobilisation e) {
        return ImmobilisationDto.builder()
            .id(e.getId())
            .code(e.getCode())
            .libelle(e.getLibelle())
            .dateAcquisition(e.getDateAcquisition())
            .valeurOrigine(e.getValeurOrigine())
            .valeurResiduelle(e.getValeurResiduelle())
            .dureeVie(e.getDureeVie())
            .methodeAmortissement(e.getMethodeAmortissement())
            .coefficientDegressif(e.getCoefficientDegressif())
            .capaciteTotaleProduction(e.getCapaciteTotaleProduction())
            .compteImmoId(e.getCompteImmoId())
            .compteAmortId(e.getCompteAmortId())
            .compteDotationId(e.getCompteDotationId())
            .statut(e.getStatut())
            .createdAt(e.getCreatedAt())
            .updatedAt(e.getUpdatedAt())
            .createdBy(e.getCreatedBy())
            .build();
    }

    private Immobilisation toEntity(ImmobilisationDto dto) {
        return Immobilisation.builder()
            .code(dto.getCode())
            .libelle(dto.getLibelle())
            .dateAcquisition(dto.getDateAcquisition())
            .valeurOrigine(dto.getValeurOrigine())
            .valeurResiduelle(dto.getValeurResiduelle() != null ? dto.getValeurResiduelle() : BigDecimal.ZERO)
            .dureeVie(dto.getDureeVie())
            .methodeAmortissement(dto.getMethodeAmortissement() != null ? dto.getMethodeAmortissement() : "LINEAIRE")
            .coefficientDegressif(dto.getCoefficientDegressif())
            .capaciteTotaleProduction(dto.getCapaciteTotaleProduction())
            .compteImmoId(dto.getCompteImmoId())
            .compteAmortId(dto.getCompteAmortId())
            .compteDotationId(dto.getCompteDotationId())
            .statut("ACTIF")
            .build();
    }
}
