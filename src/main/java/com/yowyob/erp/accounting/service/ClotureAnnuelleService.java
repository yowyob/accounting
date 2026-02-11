package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.ExerciceComptable;
import com.yowyob.erp.accounting.entity.PeriodeComptable;
import com.yowyob.erp.accounting.repository.ExerciceComptableRepository;
import com.yowyob.erp.accounting.repository.PeriodeComptableRepository;
import com.yowyob.erp.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for formal Year-End Closing (Clôture Annuelle).
 * Handles result transfer to class 13 and generation of opening entries for the
 * next year.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClotureAnnuelleService {

    private final DatabaseClient databaseClient;
    private final PeriodeComptableRepository periode_repository;
    private final ExerciceComptableRepository exercice_repository;

    /**
     * Executes the formal year-end closing process for a fiscal year.
     * 
     * @param exerciceId the ID of the fiscal year to close
     * @return Mono<Void>
     */
    @Transactional
    public Mono<Void> executerCloture(UUID exerciceId) {
        return exercice_repository.findById(exerciceId)
                .switchIfEmpty(Mono.error(new BusinessException("Exercice comptable non trouvé: " + exerciceId)))
                .flatMap(exercice -> {
                    // 1. Validation : Tous les mois doivent être clôturés
                    return periode_repository.findByExerciceId(exerciceId)
                            .all(PeriodeComptable::getCloturee)
                            .flatMap(allClosed -> {
                                if (!allClosed) {
                                    return Mono.error(new BusinessException(
                                            "L'exercice ne peut pas être clôturé car toutes les périodes mensuelles ne sont pas clôturées."));
                                }
                                return Mono.just(exercice);
                            });
                })
                .flatMap(exercice -> {
                    // 2. Transfert du résultat vers le compte 131 (Bénéfice) ou 139 (Perte)
                    return transfererResultatVersComptesFinaux(exercice)
                            .then(genererOuvertureExerciceSuivant(exercice));
                });
    }

    private Mono<Void> transfererResultatVersComptesFinaux(ExerciceComptable exercice) {
        log.info("Transfert du résultat pour l'exercice {}", exercice.getCode());

        // Calculer le solde net des classes 6 et 7 pour TOUTE l'année
        String sqlSoldes = """
                SELECT SUM(
                         CASE
                           WHEN de.sens = 'DEBIT' THEN COALESCE(de.montant_debit, 0)
                           ELSE -COALESCE(de.montant_credit, 0)
                         END
                       ) as solde_net
                FROM details_ecritures de
                JOIN comptes c ON de.compte_id = c.id
                WHERE de.organization_id = :organizationId
                  AND de.date_ecriture BETWEEN :debut AND :fin
                  AND (c.no_compte LIKE '6%' OR c.no_compte LIKE '7%')
                """;

        return databaseClient.sql(sqlSoldes)
                .bind("organizationId", exercice.getOrganizationId())
                .bind("debut", exercice.getDate_debut())
                .bind("fin", exercice.getDate_fin())
                .fetch()
                .one()
                .flatMap(row -> {
                    BigDecimal soldeNet = (BigDecimal) row.get("solde_net");
                    if (soldeNet == null || soldeNet.compareTo(BigDecimal.ZERO) == 0) {
                        return Mono.empty();
                    }

                    // Créer l'écriture de clôture de résultat
                    return creerEcritureSysteme(exercice.getOrganizationId(), exercice.getDate_fin(),
                            "Clôture de résultat annuelle " + exercice.getCode(), "OD")
                            .flatMap(ecritureId -> {
                                // Le solde net (Débits - Crédits)
                                // Si soldeNet > 0 (Déficit/Perte car charges > produits en cumul débit)
                                // Si soldeNet < 0 (Bénéfice car produits > charges en cumul crédit)
                                // Note: La requête SQL calcule (Debit - Credit). Pour class 7, Credit est
                                // positif habituellement.
                                // Attente: 7XX (Credit) - 6XX (Debit) = Benefice (>0).
                                // Ma requête fait (Debit - Credit). Donc si Credit (Produit) est dominant,
                                // soldeNet est négatif.

                                BigDecimal resultat = soldeNet.negate(); // Positif = Bénéfice, Négatif = Perte
                                String compteResultat = resultat.compareTo(BigDecimal.ZERO) >= 0 ? "131000" : "139000";
                                String sensResultat = resultat.compareTo(BigDecimal.ZERO) >= 0 ? "CREDIT" : "DEBIT";

                                return insererDetailEcriture(ecritureId, exercice.getOrganizationId(), compteResultat,
                                        resultat.abs(), sensResultat)
                                        .then(soldeComptesGestionEtTransfert(ecritureId, exercice));
                            });
                });
    }

    private Mono<Void> soldeComptesGestionEtTransfert(UUID ecritureId, ExerciceComptable exercice) {
        // Solde tous les comptes 6 et 7 vers cette écriture
        String sqlComptesGestion = """
                SELECT c.id, c.no_compte,
                       SUM(COALESCE(de.montant_debit,0) - COALESCE(de.montant_credit,0)) as solde
                FROM details_ecritures de
                JOIN comptes c ON de.compte_id = c.id
                WHERE de.organization_id = :organizationId
                  AND de.date_ecriture BETWEEN :debut AND :fin
                  AND (c.no_compte LIKE '6%' OR c.no_compte LIKE '7%')
                GROUP BY c.id, c.no_compte
                HAVING SUM(COALESCE(de.montant_debit,0) - COALESCE(de.montant_credit,0)) <> 0
                """;

        return databaseClient.sql(sqlComptesGestion)
                .bind("organizationId", exercice.getOrganizationId())
                .bind("debut", exercice.getDate_debut())
                .bind("fin", exercice.getDate_fin())
                .fetch()
                .all()
                .flatMap(row -> {
                    UUID compteId = (UUID) row.get("id");
                    BigDecimal solde = (BigDecimal) row.get("solde");
                    // Pour solder : si solde est DEBIT (>0), on CREDITE. Si solde est CREDIT (<0),
                    // on DEBITE.
                    String sens = solde.compareTo(BigDecimal.ZERO) > 0 ? "CREDIT" : "DEBIT";
                    return insererDetailEcritureDirect(ecritureId, exercice.getOrganizationId(), compteId, solde.abs(), sens);
                })
                .then();
    }

    private Mono<Void> genererOuvertureExerciceSuivant(ExerciceComptable exercice) {
        LocalDate dateOuverture = exercice.getDate_fin().plusDays(1);
        log.info("Génération des À-nouveaux au {}", dateOuverture);

        return exercice_repository.findActiveForDate(exercice.getOrganizationId(), dateOuverture)
                .flatMap(prochainExercice -> {
                    return creerEcritureSysteme(exercice.getOrganizationId(), dateOuverture,
                            "Bilan d'ouverture - À-nouveaux " + prochainExercice.getCode(), "AN")
                            .flatMap(ecritureId -> {
                                // Calculer les soldes de bilan (Classes 1 à 5) à la fin de l'exercice actuel
                                String sqlSoldesBilan = """
                                        SELECT c.id,
                                               SUM(COALESCE(de.montant_debit,0) - COALESCE(de.montant_credit,0)) as solde
                                        FROM details_ecritures de
                                        JOIN comptes c ON de.compte_id = c.id
                                        WHERE de.organization_id = :organizationId
                                          AND de.date_ecriture <= :fin
                                          AND (c.no_compte LIKE '1%' OR c.no_compte LIKE '2%' OR c.no_compte LIKE '3%' OR c.no_compte LIKE '4%' OR c.no_compte LIKE '5%')
                                        GROUP BY c.id
                                        HAVING SUM(COALESCE(de.montant_debit,0) - COALESCE(de.montant_credit,0)) <> 0
                                        """;

                                return databaseClient.sql(sqlSoldesBilan)
                                        .bind("organizationId", exercice.getOrganizationId())
                                        .bind("fin", exercice.getDate_fin())
                                        .fetch()
                                        .all()
                                        .flatMap(row -> {
                                            UUID compteId = (UUID) row.get("id");
                                            BigDecimal solde = (BigDecimal) row.get("solde");
                                            String sens = solde.compareTo(BigDecimal.ZERO) >= 0 ? "DEBIT" : "CREDIT";
                                            return insererDetailEcritureDirect(ecritureId, exercice.getOrganizationId(),
                                                    compteId, solde.abs(), sens);
                                        })
                                        .then();
                            });
                })
                .switchIfEmpty(Mono.fromRunnable(
                        () -> log.warn("Aucun exercice suivant trouvé pour générer les À-nouveaux après le {}",
                                exercice.getDate_fin())));
    }

    private Mono<UUID> creerEcritureSysteme(UUID organizationId, LocalDate date, String libelle, String codeJournal) {
        String sql = """
                INSERT INTO ecriture_comptable
                (organization_id, date_operation, libelle, journal_code, validee, created_at, numero_ecriture, journal_id, periode_id)
                VALUES (:organizationId, :date, :libelle, :code, true, NOW(), :numero, NULL, NULL)
                RETURNING id
                """;
        String numero = codeJournal + "-" + date.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8);

        return databaseClient.sql(sql)
                .bind("organizationId", organizationId)
                .bind("date", date)
                .bind("libelle", libelle)
                .bind("code", codeJournal)
                .bind("numero", numero)
                .map(row -> row.get("id", UUID.class))
                .one();
    }

    private Mono<Void> insererDetailEcriture(UUID ecritureId, UUID organizationId, String noCompte, BigDecimal montant,
            String sens) {
        String sql = """
                INSERT INTO details_ecritures
                (ecriture_id, organization_id, compte_id, libelle, sens, montant_debit, montant_credit, date_ecriture, created_at)
                SELECT :ecId, :tId, c.id, 'Clôture annuelle', :sens,
                       CASE WHEN :sens = 'DEBIT' THEN :montant ELSE 0 END,
                       CASE WHEN :sens = 'CREDIT' THEN :montant ELSE 0 END,
                       NOW(), NOW()
                FROM comptes c
                WHERE c.organization_id = :tId AND c.no_compte = :no
                """;
        return databaseClient.sql(sql)
                .bind("ecId", ecritureId)
                .bind("tId", organizationId)
                .bind("sens", sens)
                .bind("montant", montant)
                .bind("no", noCompte)
                .then();
    }

    private Mono<Void> insererDetailEcritureDirect(UUID ecritureId, UUID organizationId, UUID compteId, BigDecimal montant,
            String sens) {
        String sql = """
                INSERT INTO details_ecritures
                (ecriture_id, organization_id, compte_id, libelle, sens, montant_debit, montant_credit, date_ecriture, created_at)
                VALUES (:ecId, :tId, :cId, 'À-nouveaux (Report)', :sens,
                       CASE WHEN :sens = 'DEBIT' THEN :montant ELSE 0 END,
                       CASE WHEN :sens = 'CREDIT' THEN :montant ELSE 0 END,
                       NOW(), NOW())
                """;
        return databaseClient.sql(sql)
                .bind("ecId", ecritureId)
                .bind("tId", organizationId)
                .bind("cId", compteId)
                .bind("sens", sens)
                .bind("montant", montant)
                .then();
    }
}
