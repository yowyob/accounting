package com.yowyob.erp.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yowyob.erp.accounting.dto.RegularisationDto;
import com.yowyob.erp.accounting.entity.RegularisationComptable;
import com.yowyob.erp.accounting.entity.StatutRegularisation;
import com.yowyob.erp.accounting.entity.TypeRegularisation;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.PeriodeComptableRepository;
import com.yowyob.erp.accounting.repository.RegularisationRepository;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gestion des régularisations de fin de période OHADA.
 *
 * Quatre types pris en charge :
 *   CCA (476)     — Charges Constatées d'Avance
 *   PCA (477)     — Produits Constatés d'Avance
 *   CAP (408/428/448) — Charges À Payer
 *   PAR (418/438) — Produits À Recevoir
 *
 * Flux :
 *   1. createRegularisation() → écriture initiale + date d'extourne = 1er jour période suivante
 *   2. extournerRegularisation() (manuelle ou automatique) → écriture inverse
 *   3. Job planifié @Scheduled → extourne automatique de toutes les régularisations dues
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegularisationService {

    private final RegularisationRepository regularisationRepository;
    private final CompteRepository compteRepository;
    private final PeriodeComptableRepository periodeRepository;
    private final DatabaseClient databaseClient;

    // ────────────────────────────────────────────────
    // CRÉATION
    // ────────────────────────────────────────────────

    @Transactional
    public Mono<RegularisationDto> createRegularisation(RegularisationDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();

                return periodeRepository.findById(dto.getPeriodeId())
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Période comptable introuvable : " + dto.getPeriodeId())))
                    .flatMap(periode -> {
                        if (Boolean.TRUE.equals(periode.getCloturee())) {
                            return Mono.error(new BusinessException("La période est déjà clôturée"));
                        }
                        return compteRepository.findById(dto.getCompteChargeProduitId())
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Compte charge/produit introuvable")))
                            .then(compteRepository.findById(dto.getCompteRegularisationId())
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Compte de régularisation introuvable"))))
                            .flatMap(compteReg -> {
                                // La date d'extourne est le 1er jour du mois suivant la fin de période
                                LocalDate dateExtourne = periode.getDate_fin().plusDays(1);

                                
                                RegularisationComptable entity = RegularisationComptable.builder()
                                    .id(UUID.randomUUID())
                                    .organizationId(orgId)
                                    .typeRegularisation(dto.getTypeRegularisation())
                                    .statut(StatutRegularisation.ACTIVE)
                                    .periodeId(dto.getPeriodeId())
                                    .dateRegularisation(dto.getDateRegularisation())
                                    .compteChargeProduitId(dto.getCompteChargeProduitId())
                                    .compteRegularisationId(dto.getCompteRegularisationId())
                                    .montant(dto.getMontant())
                                    .libelle(dto.getLibelle())
                                    .notes(dto.getNotes())
                                    .dateExtourne(dateExtourne)
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .createdBy(user)
                                    .build();

                                return creerEcritureInitiale(entity, orgId, user)
                                    .flatMap(ecritureId -> {
                                        entity.setEcritureInitialeId(ecritureId);
                                        return regularisationRepository.save(entity);
                                    })
                                    .map(this::toDto);
                            });
                    });
            });
    }

    // ────────────────────────────────────────────────
    // LECTURE
    // ────────────────────────────────────────────────

    public Flux<RegularisationDto> getAll() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> regularisationRepository.findByOrganizationId(orgId)
                .map(this::toDto));
    }

    public Mono<RegularisationDto> getById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> regularisationRepository.findByOrganizationIdAndId(orgId, id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Régularisation introuvable : " + id)))
                .map(this::toDto));
    }

    public Flux<RegularisationDto> getByPeriode(UUID periodeId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> regularisationRepository.findByOrganizationIdAndPeriodeId(orgId, periodeId)
                .map(this::toDto));
    }

    public Flux<RegularisationDto> getByType(TypeRegularisation type) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> regularisationRepository.findByOrganizationIdAndType(orgId, type.name())
                .map(this::toDto));
    }

    public Flux<RegularisationDto> getByStatut(StatutRegularisation statut) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> regularisationRepository.findByOrganizationIdAndStatut(orgId, statut.name())
                .map(this::toDto));
    }

    // ────────────────────────────────────────────────
    // EXTOURNE MANUELLE
    // ────────────────────────────────────────────────

    @Transactional
    public Mono<RegularisationDto> extourner(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                return regularisationRepository.findByOrganizationIdAndId(orgId, id)
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Régularisation introuvable : " + id)))
                    .flatMap(reg -> appliquerExtourne(reg, orgId, user))
                    .map(this::toDto);
            });
    }

    /**
     * Extourne toutes les régularisations actives dont la date d'extourne est arrivée
     * pour une organisation donnée. Appelable manuellement à l'ouverture d'une nouvelle période.
     */
    @Transactional
    public Mono<Long> extournerDues(UUID orgId, String user) {
        return regularisationRepository.findDuesForExtourne(orgId, LocalDate.now())
            .flatMap(reg -> appliquerExtourne(reg, orgId, user))
            .count()
            .doOnSuccess(count -> log.info("✅ {} régularisation(s) extournée(s) pour organisation {}", count, orgId));
    }

    // ────────────────────────────────────────────────
    // ANNULATION
    // ────────────────────────────────────────────────

    @Transactional
    public Mono<Void> annuler(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                return regularisationRepository.findByOrganizationIdAndId(orgId, id)
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Régularisation introuvable : " + id)))
                    .flatMap(reg -> {
                        if (reg.getStatut() != StatutRegularisation.ACTIVE) {
                            return Mono.error(new BusinessException(
                                "Seule une régularisation ACTIVE peut être annulée (statut actuel : " + reg.getStatut() + ")"));
                        }
                        reg.setStatut(StatutRegularisation.ANNULEE);
                        reg.setUpdatedAt(LocalDateTime.now());
                        reg.setUpdatedBy(user);
                        return regularisationRepository.save(reg).then();
                    });
            });
    }

    // ────────────────────────────────────────────────
    // JOB AUTOMATIQUE — s'exécute chaque jour à 01h00
    // ────────────────────────────────────────────────

    @Scheduled(cron = "0 0 1 * * *")
    public void jobExtourneAutomatique() {
        log.info("⏰ Démarrage du job d'extourne automatique des régularisations...");
        regularisationRepository.findAllDuesForExtourne(LocalDate.now())
            .flatMap(reg -> appliquerExtourne(reg, reg.getOrganizationId(), "system-job"))
            .count()
            .subscribe(
                count -> log.info("⏰ Job terminé : {} régularisation(s) extournée(s)", count),
                error -> log.error("⏰ Erreur dans le job d'extourne automatique : {}", error.getMessage())
            );
    }

    // ────────────────────────────────────────────────
    // LOGIQUE INTERNE
    // ────────────────────────────────────────────────

    private Mono<RegularisationComptable> appliquerExtourne(RegularisationComptable reg, UUID orgId, String user) {
        if (reg.getStatut() != StatutRegularisation.ACTIVE) {
            log.warn("Régularisation {} déjà extournée ou annulée, ignorée", reg.getId());
            return Mono.just(reg);
        }
        return creerEcritureExtourne(reg, orgId, user)
            .flatMap(ecritureExtourneId -> {
                reg.setEcritureExtourneId(ecritureExtourneId);
                reg.setStatut(StatutRegularisation.EXTOURNEE);
                reg.setExtourneePar(user);
                reg.setDateExtourneEffective(LocalDateTime.now());
                reg.setUpdatedAt(LocalDateTime.now());
                reg.setUpdatedBy(user);
                return regularisationRepository.save(reg);
            });
    }

    /**
     * Génère l'écriture initiale selon le type :
     *   CCA : Débit 476 / Crédit 6xx
     *   PCA : Débit 7xx / Crédit 477
     *   CAP : Débit 6xx / Crédit 408|428|448
     *   PAR : Débit 418|438 / Crédit 7xx
     */
    private Mono<UUID> creerEcritureInitiale(RegularisationComptable reg, UUID orgId, String user) {
        String libelle = "[REG-" + reg.getTypeRegularisation() + "] " + reg.getLibelle();
        String prefix = "OD-REG-" + reg.getTypeRegularisation().name();

        return creerEcritureSysteme(orgId, reg.getDateRegularisation(), libelle, prefix, user)
            .flatMap(ecritureId -> {
                UUID debitId;
                UUID creditId;
                switch (reg.getTypeRegularisation()) {
                    case CCA -> { debitId = reg.getCompteRegularisationId(); creditId = reg.getCompteChargeProduitId(); }
                    case PCA -> { debitId = reg.getCompteChargeProduitId(); creditId = reg.getCompteRegularisationId(); }
                    case CAP -> { debitId = reg.getCompteChargeProduitId(); creditId = reg.getCompteRegularisationId(); }
                    case PAR -> { debitId = reg.getCompteRegularisationId(); creditId = reg.getCompteChargeProduitId(); }
                    default  -> throw new BusinessException("Type de régularisation inconnu : " + reg.getTypeRegularisation());
                }
                return Mono.when(
                    insererDetailEcriture(ecritureId, orgId, debitId, reg.getMontant(), "DEBIT", libelle),
                    insererDetailEcriture(ecritureId, orgId, creditId, reg.getMontant(), "CREDIT", libelle)
                ).thenReturn(ecritureId);
            });
    }

    /**
     * Génère l'écriture d'extourne (sens inversé de l'écriture initiale).
     *   CCA extourne : Débit 6xx / Crédit 476
     *   PCA extourne : Débit 477 / Crédit 7xx
     *   CAP extourne : Débit 408|428|448 / Crédit 6xx
     *   PAR extourne : Débit 7xx / Crédit 418|438
     */
    private Mono<UUID> creerEcritureExtourne(RegularisationComptable reg, UUID orgId, String user) {
        LocalDate dateExtourne = LocalDate.now();
        String libelle = "[EXTOURNE-" + reg.getTypeRegularisation() + "] " + reg.getLibelle();
        String prefix = "OD-EXT-" + reg.getTypeRegularisation().name();

        return creerEcritureSysteme(orgId, dateExtourne, libelle, prefix, user)
            .flatMap(ecritureId -> {
                UUID debitId;
                UUID creditId;
                // Sens inversé
                switch (reg.getTypeRegularisation()) {
                    case CCA -> { debitId = reg.getCompteChargeProduitId(); creditId = reg.getCompteRegularisationId(); }
                    case PCA -> { debitId = reg.getCompteRegularisationId(); creditId = reg.getCompteChargeProduitId(); }
                    case CAP -> { debitId = reg.getCompteRegularisationId(); creditId = reg.getCompteChargeProduitId(); }
                    case PAR -> { debitId = reg.getCompteChargeProduitId(); creditId = reg.getCompteRegularisationId(); }
                    default  -> throw new BusinessException("Type de régularisation inconnu : " + reg.getTypeRegularisation());
                }
                return Mono.when(
                    insererDetailEcriture(ecritureId, orgId, debitId, reg.getMontant(), "DEBIT", libelle),
                    insererDetailEcriture(ecritureId, orgId, creditId, reg.getMontant(), "CREDIT", libelle)
                ).thenReturn(ecritureId);
            });
    }

    private Mono<UUID> creerEcritureSysteme(UUID orgId, LocalDate date, String libelle, String prefix, String user) {
        UUID id = UUID.randomUUID();
        String numero = prefix + "-" + id.toString().substring(0, 8).toUpperCase();
        String sql = """
            INSERT INTO ecritures_comptables
              (id, organization_id, date_ecriture, libelle, validee, created_at, updated_at, numero_ecriture, created_by)
            VALUES
              (:id, :orgId, :date, :libelle, true, NOW(), NOW(), :numero, :user)
            RETURNING id
            """;
        return databaseClient.sql(sql)
            .bind("id", id)
            .bind("orgId", orgId)
            .bind("date", date)
            .bind("libelle", libelle)
            .bind("numero", numero)
            .bind("user", user)
            .map(row -> row.get("id", UUID.class))
            .one();
    }

    private Mono<Void> insererDetailEcriture(UUID ecritureId, UUID orgId, UUID compteId,
                                              BigDecimal montant, String sens, String libelle) {
        String sql = """
            INSERT INTO details_ecritures
              (id, ecriture_id, organization_id, compte_id, libelle, sens,
               montant_debit, montant_credit, date_ecriture, created_at, updated_at)
            VALUES
              (:id, :ecId, :orgId, :compteId, :libelle, :sens,
               CASE WHEN :sens = 'DEBIT'  THEN :montant ELSE 0 END,
               CASE WHEN :sens = 'CREDIT' THEN :montant ELSE 0 END,
               NOW(), NOW(), NOW())
            """;
        return databaseClient.sql(sql)
            .bind("id", UUID.randomUUID())
            .bind("ecId", ecritureId)
            .bind("orgId", orgId)
            .bind("compteId", compteId)
            .bind("libelle", libelle)
            .bind("sens", sens)
            .bind("montant", montant)
            .then();
    }

    // ────────────────────────────────────────────────
    // MAPPING
    // ────────────────────────────────────────────────

    private RegularisationDto toDto(RegularisationComptable e) {
        return RegularisationDto.builder()
            .id(e.getId())
            .typeRegularisation(e.getTypeRegularisation())
            .statut(e.getStatut())
            .periodeId(e.getPeriodeId())
            .dateRegularisation(e.getDateRegularisation())
            .compteChargeProduitId(e.getCompteChargeProduitId())
            .compteRegularisationId(e.getCompteRegularisationId())
            .montant(e.getMontant())
            .libelle(e.getLibelle())
            .notes(e.getNotes())
            .ecritureInitialeId(e.getEcritureInitialeId())
            .dateExtourne(e.getDateExtourne())
            .ecritureExtourneId(e.getEcritureExtourneId())
            .extourneePar(e.getExtourneePar())
            .dateExtourneEffective(e.getDateExtourneEffective())
            .createdAt(e.getCreatedAt())
            .updatedAt(e.getUpdatedAt())
            .createdBy(e.getCreatedBy())
            .updatedBy(e.getUpdatedBy())
            .build();
    }
}
