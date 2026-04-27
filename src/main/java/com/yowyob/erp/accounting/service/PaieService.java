package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.LignePaieDto;
import com.yowyob.erp.accounting.entity.LignePaie;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.LignePaieRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module Paie : saisie des bulletins de salaires agrégés par mois
 * et génération automatique des écritures comptables OHADA.
 *
 * Plan comptable utilisé (OHADA SYSCOHADA) :
 *   6611 — Traitements et salaires bruts
 *   4311 — CNPS, cotisations retenues (part salariale)
 *   4441 — Impôts sur traitements et salaires (IRPP)
 *   4220 — Personnel — Rémunérations dues (salaire net à payer)
 *   6613 — Charges sociales et fiscales (charges patronales)
 *   4312 — CNPS, cotisations patronales
 *
 * Écriture générée lors de la comptabilisation :
 *   D 6611  Salaire brut
 *   C 4311  Retenue CNPS salariée
 *   C 4441  Retenue IRPP
 *   C 4220  Salaire net à payer  (= brut - CNPS - IRPP - autres retenues)
 *
 *   D 6613  Charges patronales CNPS
 *   C 4312  CNPS patronale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaieService {

    private final LignePaieRepository paie_repository;
    private final CompteRepository compte_repository;
    private final DatabaseClient databaseClient;

    // Numéros de comptes OHADA par défaut — l'organisation peut les surcharger
    private static final String NO_6611 = "6611";
    private static final String NO_4311 = "4311";
    private static final String NO_4441 = "4441";
    private static final String NO_4220 = "4220";
    private static final String NO_6613 = "6613";
    private static final String NO_4312 = "4312";

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    @Transactional
    public Mono<LignePaieDto> create(LignePaieDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();

                BigDecimal net = dto.getSalaireNetTotal() != null
                    ? dto.getSalaireNetTotal()
                    : dto.getSalaireBrutTotal()
                        .subtract(orZero(dto.getRetenueCnpsSalarie()))
                        .subtract(orZero(dto.getRetenueIrpp()))
                        .subtract(orZero(dto.getAutresRetenues()));

                LignePaie entity = LignePaie.builder()
                    .id(UUID.randomUUID())
                    .organizationId(orgId)
                    .exerciceId(dto.getExerciceId())
                    .periodeId(dto.getPeriodeId())
                    .moisPaie(dto.getMoisPaie())
                    .libelle(dto.getLibelle())
                    .salaireBrutTotal(dto.getSalaireBrutTotal())
                    .retenueCnpsSalarie(orZero(dto.getRetenueCnpsSalarie()))
                    .retenueIrpp(orZero(dto.getRetenueIrpp()))
                    .autresRetenues(orZero(dto.getAutresRetenues()))
                    .salaireNetTotal(net)
                    .chargePatronaleCnps(orZero(dto.getChargePatronaleCnps()))
                    .autresChargesPatronales(orZero(dto.getAutresChargesPatronales()))
                    .statut("BROUILLON")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy(user)
                    .updatedBy(user)
                    .build();

                return paie_repository.save(entity).map(this::toDto);
            });
    }

    @Transactional
    public Mono<LignePaieDto> update(UUID id, LignePaieDto dto) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                return paie_repository.findByOrganizationIdAndId(orgId, id)
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("LignePaie", id.toString())))
                    .flatMap(existing -> {
                        if ("COMPTABILISE".equals(existing.getStatut())) {
                            return Mono.error(new BusinessException("Un bulletin comptabilisé ne peut plus être modifié."));
                        }
                        BigDecimal net = dto.getSalaireNetTotal() != null
                            ? dto.getSalaireNetTotal()
                            : dto.getSalaireBrutTotal()
                                .subtract(orZero(dto.getRetenueCnpsSalarie()))
                                .subtract(orZero(dto.getRetenueIrpp()))
                                .subtract(orZero(dto.getAutresRetenues()));

                        existing.setMoisPaie(dto.getMoisPaie());
                        existing.setLibelle(dto.getLibelle());
                        existing.setSalaireBrutTotal(dto.getSalaireBrutTotal());
                        existing.setRetenueCnpsSalarie(orZero(dto.getRetenueCnpsSalarie()));
                        existing.setRetenueIrpp(orZero(dto.getRetenueIrpp()));
                        existing.setAutresRetenues(orZero(dto.getAutresRetenues()));
                        existing.setSalaireNetTotal(net);
                        existing.setChargePatronaleCnps(orZero(dto.getChargePatronaleCnps()));
                        existing.setAutresChargesPatronales(orZero(dto.getAutresChargesPatronales()));
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();
                        return paie_repository.save(existing).map(this::toDto);
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> paie_repository.findByOrganizationIdAndId(orgId, id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("LignePaie", id.toString())))
                .flatMap(existing -> {
                    if ("COMPTABILISE".equals(existing.getStatut())) {
                        return Mono.error(new BusinessException("Impossible de supprimer un bulletin comptabilisé."));
                    }
                    return paie_repository.delete(existing);
                }));
    }

    public Mono<LignePaieDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> paie_repository.findByOrganizationIdAndId(orgId, id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("LignePaie", id.toString())))
                .map(this::toDto));
    }

    public Flux<LignePaieDto> findByExercice(UUID exerciceId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> paie_repository.findByOrganizationIdAndExerciceId(orgId, exerciceId)
                .map(this::toDto));
    }

    public Flux<LignePaieDto> findByPeriode(UUID periodeId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> paie_repository.findByOrganizationIdAndPeriodeId(orgId, periodeId)
                .map(this::toDto));
    }

    // ─────────────────────────────────────────────
    // COMPTABILISATION
    // ─────────────────────────────────────────────

    /**
     * Génère les écritures comptables OHADA pour un bulletin de paie.
     * Le bulletin passe de BROUILLON/VALIDE à COMPTABILISE.
     */
    @Transactional
    public Mono<LignePaieDto> comptabiliser(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> {
                UUID orgId = tuple.getT1();
                String user = tuple.getT2();
                return paie_repository.findByOrganizationIdAndId(orgId, id)
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("LignePaie", id.toString())))
                    .flatMap(paie -> {
                        if ("COMPTABILISE".equals(paie.getStatut())) {
                            return Mono.error(new BusinessException("Ce bulletin est déjà comptabilisé."));
                        }

                        String libEcriture = "Paie " + (paie.getLibelle() != null ? paie.getLibelle()
                            : paie.getMoisPaie().toString());

                        return creerEcritureSysteme(orgId, paie.getMoisPaie(), libEcriture)
                            .flatMap(ecritureId -> genererDetailsEcriturePaie(ecritureId, orgId, paie)
                                .then(Mono.defer(() -> {
                                    paie.setStatut("COMPTABILISE");
                                    paie.setEcritureId(ecritureId);
                                    paie.setUpdatedAt(LocalDateTime.now());
                                    paie.setUpdatedBy(user);
                                    paie.setNotNew();
                                    return paie_repository.save(paie).map(this::toDto);
                                })));
                    });
            });
    }

    // ─────────────────────────────────────────────
    // ÉCRITURES OHADA
    // ─────────────────────────────────────────────

    private Mono<Void> genererDetailsEcriturePaie(UUID ecritureId, UUID orgId, LignePaie paie) {
        return compte_repository.findAllByOrganization_Id(orgId).collectList()
            .flatMap(comptes -> {
                java.util.Map<String, UUID> compteIdByNo = new java.util.HashMap<>();
                for (var c : comptes) {
                    if (c.getNo_compte() != null) compteIdByNo.put(c.getNo_compte(), c.getId());
                }

                UUID id6611 = compteIdByNo.get(NO_6611);
                UUID id4311 = compteIdByNo.get(NO_4311);
                UUID id4441 = compteIdByNo.get(NO_4441);
                UUID id4220 = compteIdByNo.get(NO_4220);
                UUID id6613 = compteIdByNo.get(NO_6613);
                UUID id4312 = compteIdByNo.get(NO_4312);

                if (id6611 == null || id4220 == null) {
                    return Mono.error(new BusinessException(
                        "Comptes de paie non configurés. Vérifiez les comptes 6611 et 4220 dans le plan comptable."));
                }

                BigDecimal brut = paie.getSalaireBrutTotal();
                BigDecimal cnpsSal = orZero(paie.getRetenueCnpsSalarie());
                BigDecimal irpp = orZero(paie.getRetenueIrpp());
                BigDecimal autres = orZero(paie.getAutresRetenues());
                BigDecimal net = paie.getSalaireNetTotal();
                BigDecimal cnpsPatron = orZero(paie.getChargePatronaleCnps());
                BigDecimal autresCharges = orZero(paie.getAutresChargesPatronales());

                // Écriture 1 : Salaires
                // D 6611 Salaire brut
                // C 4311 CNPS salarié
                // C 4441 IRPP
                // C 4220 Net à payer
                var ops = new java.util.ArrayList<Mono<Void>>();
                ops.add(insertDetail(ecritureId, orgId, id6611, brut, "DEBIT", "Salaires bruts"));
                if (id4311 != null && cnpsSal.compareTo(BigDecimal.ZERO) > 0)
                    ops.add(insertDetail(ecritureId, orgId, id4311, cnpsSal, "CREDIT", "CNPS salarié"));
                if (id4441 != null && irpp.compareTo(BigDecimal.ZERO) > 0)
                    ops.add(insertDetail(ecritureId, orgId, id4441, irpp, "CREDIT", "IRPP"));
                if (autres.compareTo(BigDecimal.ZERO) > 0)
                    ops.add(insertDetail(ecritureId, orgId, id4220, autres, "CREDIT", "Autres retenues"));
                ops.add(insertDetail(ecritureId, orgId, id4220, net, "CREDIT", "Salaire net à payer"));

                // Écriture 2 : Charges patronales
                if (id6613 != null && id4312 != null) {
                    BigDecimal totalPatron = cnpsPatron.add(autresCharges);
                    if (totalPatron.compareTo(BigDecimal.ZERO) > 0) {
                        ops.add(insertDetail(ecritureId, orgId, id6613, totalPatron, "DEBIT", "Charges patronales"));
                        ops.add(insertDetail(ecritureId, orgId, id4312, cnpsPatron, "CREDIT", "CNPS patronal"));
                        if (autresCharges.compareTo(BigDecimal.ZERO) > 0)
                            ops.add(insertDetail(ecritureId, orgId, id4312, autresCharges, "CREDIT", "Autres charges patronales"));
                    }
                }

                return Mono.when(ops);
            });
    }

    private Mono<Void> insertDetail(UUID ecritureId, UUID orgId, UUID compteId,
            BigDecimal montant, String sens, String libelle) {
        String sql = """
            INSERT INTO details_ecritures
            (id, ecriture_id, organization_id, compte_id, libelle, sens,
             montant_debit, montant_credit, date_ecriture, created_at)
            VALUES (:id, :ecId, :orgId, :cId, :lib, :sens,
                CASE WHEN :sens = 'DEBIT' THEN :montant ELSE 0 END,
                CASE WHEN :sens = 'CREDIT' THEN :montant ELSE 0 END,
                NOW(), NOW())
            """;
        return databaseClient.sql(sql)
            .bind("id", UUID.randomUUID())
            .bind("ecId", ecritureId)
            .bind("orgId", orgId)
            .bind("cId", compteId)
            .bind("lib", libelle)
            .bind("sens", sens)
            .bind("montant", montant)
            .then();
    }

    private Mono<UUID> creerEcritureSysteme(UUID orgId, LocalDate date, String libelle) {
        UUID id = UUID.randomUUID();
        String numero = "PAIE-" + date.toString().substring(0, 7) + "-" + id.toString().substring(0, 8);
        String sql = """
            INSERT INTO ecritures_comptables
            (id, organization_id, date_ecriture, libelle, journal_id, validee, created_at, numero_ecriture)
            VALUES (:id, :orgId, :date, :lib, NULL, true, NOW(), :numero)
            RETURNING id
            """;
        return databaseClient.sql(sql)
            .bind("id", id)
            .bind("orgId", orgId)
            .bind("date", date)
            .bind("lib", libelle)
            .bind("numero", numero)
            .map(row -> row.get("id", UUID.class))
            .one();
    }

    // ─────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private LignePaieDto toDto(LignePaie e) {
        return LignePaieDto.builder()
            .id(e.getId())
            .exerciceId(e.getExerciceId())
            .periodeId(e.getPeriodeId())
            .moisPaie(e.getMoisPaie())
            .libelle(e.getLibelle())
            .salaireBrutTotal(e.getSalaireBrutTotal())
            .retenueCnpsSalarie(e.getRetenueCnpsSalarie())
            .retenueIrpp(e.getRetenueIrpp())
            .autresRetenues(e.getAutresRetenues())
            .salaireNetTotal(e.getSalaireNetTotal())
            .chargePatronaleCnps(e.getChargePatronaleCnps())
            .autresChargesPatronales(e.getAutresChargesPatronales())
            .statut(e.getStatut())
            .ecritureId(e.getEcritureId())
            .createdAt(e.getCreatedAt())
            .createdBy(e.getCreatedBy())
            .build();
    }
}
