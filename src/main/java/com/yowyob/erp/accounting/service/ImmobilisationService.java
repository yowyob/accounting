package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.AmortissementLigne;
import com.yowyob.erp.accounting.entity.ExerciceComptable;
import com.yowyob.erp.accounting.entity.Immobilisation;
import com.yowyob.erp.accounting.repository.AmortissementLigneRepository;
import com.yowyob.erp.accounting.repository.ExerciceComptableRepository;
import com.yowyob.erp.accounting.repository.ImmobilisationRepository;
import com.yowyob.erp.common.exception.BusinessException;
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
 * Service for Fixed Asset Management and Depreciation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImmobilisationService {

    private final ImmobilisationRepository immo_repository;
    private final AmortissementLigneRepository amort_repository;
    private final ExerciceComptableRepository exercice_repository;
    private final DatabaseClient databaseClient;

    /**
     * Generates the depreciation schedule for an asset using linear method.
     */
    @Transactional
    public Mono<Void> genererTableauAmortissement(UUID immoId) {
        return immo_repository.findById(immoId)
                .flatMap(immo -> {
                    if (!"LINEAIRE".equals(immo.getMethodeAmortissement())) {
                        return Mono.error(new BusinessException(
                                "Méthode d'amortissement non supportée: " + immo.getMethodeAmortissement()));
                    }

                    return amort_repository.findByImmoId(immoId).then(Mono.just(immo));
                })
                .flatMap(immo -> {
                    List<AmortissementLigne> schedule = new ArrayList<>();
                    BigDecimal base = immo.getValeurOrigine().subtract(immo.getValeurResiduelle());
                    BigDecimal annualRate = BigDecimal.valueOf(100).divide(BigDecimal.valueOf(immo.getDureeVie()), 2,
                            RoundingMode.HALF_UP);
                    BigDecimal annualAnnuity = base.multiply(annualRate).divide(BigDecimal.valueOf(100), 2,
                            RoundingMode.HALF_UP);

                    LocalDate currentYearStart = immo.getDateAcquisition().withDayOfYear(1);
                    BigDecimal cumul = BigDecimal.ZERO;

                    for (int year = 0; year <= immo.getDureeVie(); year++) {
                        LocalDate endOfYear = currentYearStart.plusYears(year).withMonth(12).withDayOfMonth(31);
                        BigDecimal annuity;

                        if (year == 0) {
                            // Prorata temporis for the first year
                            long days = ChronoUnit.DAYS.between(immo.getDateAcquisition(), endOfYear) + 1;
                            annuity = annualAnnuity.multiply(BigDecimal.valueOf(days))
                                    .divide(BigDecimal.valueOf(360), 2, RoundingMode.HALF_UP);
                        } else if (year == immo.getDureeVie()) {
                            // Last year to adjust to base
                            annuity = base.subtract(cumul);
                        } else {
                            annuity = annualAnnuity;
                        }

                        if (annuity.compareTo(BigDecimal.ZERO) <= 0)
                            continue;

                        cumul = cumul.add(annuity);
                        schedule.add(AmortissementLigne.builder()
                                .id(UUID.randomUUID())
                                .immoId(immo.getId())
                                .dateEcheance(endOfYear)
                                .baseCalcul(base)
                                .taux(annualRate)
                                .annuite(annuity)
                                .cumulAmortissement(cumul)
                                .valeurNetteComptable(immo.getValeurOrigine().subtract(cumul))
                                .comptabilisee(false)
                                .build());

                        if (cumul.compareTo(base) >= 0)
                            break;
                    }

                    return amort_repository.saveAll(schedule).then();
                });
    }

    /**
     * Generates accounting entries for all depreciation lines due in a fiscal
     * period.
     */
    @Transactional
    public Mono<Void> comptabiliserAmortissements(UUID exerciceId) {
        return exercice_repository.findById(exerciceId)
                .flatMapMany(ex -> amort_repository.findByExerciceIdAndComptabiliseeFalse(exerciceId)
                        .flatMap(ligne -> immo_repository.findById(ligne.getImmoId())
                                .flatMap(immo -> creerEcritureAmortissement(ex, immo, ligne))))
                .then();
    }

    private Mono<Void> creerEcritureAmortissement(ExerciceComptable ex, Immobilisation immo, AmortissementLigne ligne) {
        String libelle = "Dotation amortissement " + ex.getCode() + " - " + immo.getLibelle();
        return creerEcritureSysteme(ex.getTenantId(), ex.getDate_fin(), libelle, "OD")
                .flatMap(ecritureId -> {
                    // Debit 681 (Dotations)
                    // Credit 281 (Amortissements)
                    return Mono.when(
                            insererDetailEcritureDirect(ecritureId, ex.getTenantId(), immo.getCompteDotationId(),
                                    ligne.getAnnuite(), "DEBIT"),
                            insererDetailEcritureDirect(ecritureId, ex.getTenantId(), immo.getCompteAmortId(),
                                    ligne.getAnnuite(), "CREDIT"))
                            .then(Mono.defer(() -> {
                                ligne.setComptabilisee(true);
                                ligne.setEcritureId(ecritureId);
                                return amort_repository.save(ligne);
                            }));
                }).then();
    }

    private Mono<UUID> creerEcritureSysteme(UUID tenantId, LocalDate date, String libelle, String codeJournal) {
        String sql = """
                INSERT INTO ecritures_comptables
                (id, tenant_id, date_ecriture, libelle, journal_id, validee, created_at, numero_ecriture)
                VALUES (:id, :tenantId, :date, :libelle, NULL, true, NOW(), :numero)
                RETURNING id
                """;
        UUID id = UUID.randomUUID();
        String numero = codeJournal + "-AMORT-" + UUID.randomUUID().toString().substring(0, 8);

        return databaseClient.sql(sql)
                .bind("id", id)
                .bind("tenantId", tenantId)
                .bind("date", date)
                .bind("libelle", libelle)
                .bind("numero", numero)
                .map(row -> row.get("id", UUID.class))
                .one();
    }

    private Mono<Void> insererDetailEcritureDirect(UUID ecritureId, UUID tenantId, UUID compteId, BigDecimal montant,
            String sens) {
        String sql = """
                INSERT INTO details_ecritures
                (id, ecriture_id, tenant_id, compte_id, libelle, sens, montant_debit, montant_credit, date_ecriture, created_at)
                VALUES (:id, :ecId, :tId, :cId, 'Dotation aux amortissements', :sens,
                       CASE WHEN :sens = 'DEBIT' THEN :montant ELSE 0 END,
                       CASE WHEN :sens = 'CREDIT' THEN :montant ELSE 0 END,
                       NOW(), NOW())
                """;
        return databaseClient.sql(sql)
                .bind("id", UUID.randomUUID())
                .bind("ecId", ecritureId)
                .bind("tId", tenantId)
                .bind("cId", compteId)
                .bind("sens", sens)
                .bind("montant", montant)
                .then();
    }
}
