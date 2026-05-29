package com.yowyob.erp.accounting.application.service;
import com.yowyob.erp.accounting.domain.port.in.ClotureMensuelleUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

/**
 * Service for monthly closing and generation of new annual entries.
 * Refactored to Reactive (R2DBC).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClotureMensuelleService implements ClotureMensuelleUseCase {

  private final DatabaseClient databaseClient;

  /**
   * Clôture le mois donné et génère les À-nouveaux pour le mois suivant
   * Conforme OHADA : solde des comptes de charges/produits → compte 120
   * (Résultat)
   * Puis création automatique des écritures À-nouveaux au 01 du mois suivant
   */
  @Transactional
  public Mono<Void> cloturerMoisEtGenererANouveaux(UUID organizationId, int mois, int annee) {
    YearMonth moisCloture = YearMonth.of(annee, mois);
    LocalDate dateCloture = moisCloture.atEndOfMonth();
    LocalDate dateANouveaux = moisCloture.plusMonths(1).atDay(1);

    // Étape 1 : Création de l'écriture de clôture
    return creerEcritureCloture(organizationId, dateCloture)
        .flatMap(ecritureClotureId -> {
          // Étape 2 : Calcul des soldes des comptes 6 et 7
          return calculerSoldesClasses67(organizationId, mois, annee)
              .collectList()
              .flatMap(soldes -> {
                java.util.concurrent.atomic.AtomicReference<BigDecimal> totalCharges = new java.util.concurrent.atomic.AtomicReference<>(
                    BigDecimal.ZERO);
                java.util.concurrent.atomic.AtomicReference<BigDecimal> totalProduits = new java.util.concurrent.atomic.AtomicReference<>(
                    BigDecimal.ZERO);

                Flux<Void> inserts = Flux.empty();

                for (Map<String, Object> row : soldes) {
                  String numeroCompte = (String) row.get("numero");
                  BigDecimal solde = (BigDecimal) row.get("solde");

                  if (numeroCompte.startsWith("6")) {
                    totalCharges.updateAndGet(v -> v.add(solde));
                    inserts = inserts
                        .concatWith(insererDetailEcriture(ecritureClotureId, organizationId, numeroCompte, solde, "CREDIT"));
                  } else if (numeroCompte.startsWith("7")) {
                    totalProduits.updateAndGet(v -> v.add(solde));
                    inserts = inserts
                        .concatWith(insererDetailEcriture(ecritureClotureId, organizationId, numeroCompte, solde, "DEBIT"));
                  }
                }

                return inserts.then(Mono.defer(() -> {
                  // Écriture finale : Report du résultat sur le compte 120
                  BigDecimal tCharges = totalCharges.get();
                  BigDecimal tProduits = totalProduits.get();
                  BigDecimal resultat = tProduits.subtract(tCharges);
                  String compteResultat = resultat.compareTo(BigDecimal.ZERO) >= 0 ? "120000" : "121000";
                  String sens = resultat.compareTo(BigDecimal.ZERO) >= 0 ? "DEBIT" : "CREDIT";
                  return insererDetailEcriture(ecritureClotureId, organizationId, compteResultat, resultat.abs(), sens);
                }));
              });
        })
        .then(genererEcrituresANouveaux(organizationId, dateANouveaux));
  }

  private Mono<UUID> creerEcritureCloture(UUID organizationId, LocalDate date) {
    String sql = """
        INSERT INTO ecriture_comptable
        (organization_id, date_operation, libelle, journal_code, validee, created_at, numero_ecriture, journal_id, periode_id)
        VALUES (:organizationId, :date, :libelle, 'OD', true, NOW(), :numeroEcriture, NULL, NULL)
        RETURNING id
        """;

    return databaseClient.sql(sql)
        .bind("organizationId", organizationId)
        .bind("date", date)
        .bind("libelle", "Clôture mois " + date.getMonthValue() + "/" + date.getYear())
        .bind("numeroEcriture",
            "CLOT-" + date.getYear() + "-" + date.getMonthValue() + "-" + UUID.randomUUID().toString().substring(0, 8))
        .map(row -> row.get("id", UUID.class))
        .one();
  }

  private Flux<Map<String, Object>> calculerSoldesClasses67(UUID organizationId, int mois, int annee) {
    String sql = """
        SELECT c.numero,
               SUM(
                 CASE
                   WHEN de.sens = 'DEBIT' THEN COALESCE(de.montant_debit, 0)
                   ELSE -COALESCE(de.montant_credit, 0)
                 END
               ) as solde
        FROM details_ecritures de
        JOIN comptes c ON de.compte_id = c.id
        WHERE de.organization_id = :organizationId
          AND EXTRACT(MONTH FROM de.date_ecriture) = :mois
          AND EXTRACT(YEAR FROM de.date_ecriture) = :annee
          AND (c.numero LIKE '6%' OR c.numero LIKE '7%')
        GROUP BY c.numero
        HAVING SUM(
                 CASE
                   WHEN de.sens = 'DEBIT' THEN COALESCE(de.montant_debit, 0)
                   ELSE -COALESCE(de.montant_credit, 0)
                 END
               ) <> 0
        """;

    return databaseClient.sql(sql)
        .bind("organizationId", organizationId)
        .bind("mois", mois)
        .bind("annee", annee)
        .fetch()
        .all();
  }

  private Mono<Void> insererDetailEcriture(UUID ecritureId, UUID organizationId, String numeroCompte, BigDecimal montant,
      String sens) {
    String sql = """
        INSERT INTO details_ecritures
        (ecriture_id, organization_id, compte_id, libelle, sens, montant_debit, montant_credit, date_ecriture, created_at)
        SELECT :ecritureId, :organizationId, c.id, 'Clôture automatique', :sens,
               CASE WHEN :sens = 'DEBIT' THEN :montant ELSE NULL END,
               CASE WHEN :sens = 'CREDIT' THEN :montant ELSE NULL END,
               NOW(), NOW()
        FROM comptes c
        WHERE c.organization_id = :organizationId AND c.numero = :numeroCompte
        """;

    return databaseClient.sql(sql)
        .bind("ecritureId", ecritureId)
        .bind("organizationId", organizationId)
        .bind("sens", sens)
        .bind("montant", montant)
        .bind("numeroCompte", numeroCompte)
        .then();
  }

  private Mono<Void> genererEcrituresANouveaux(UUID organizationId, LocalDate dateDebut) {
    String sql = """
        INSERT INTO ecriture_comptable (organization_id, date_operation, libelle, journal_code, validee, created_at, numero_ecriture, journal_id, periode_id)
        VALUES (:organizationId, :dateDebut, 'À-nouveaux', 'AN', true, NOW(), :numeroEcriture, NULL, NULL)
        RETURNING id
        """;

    return databaseClient.sql(sql)
        .bind("organizationId", organizationId)
        .bind("dateDebut", dateDebut)
        .bind("numeroEcriture", "AN-" + dateDebut.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8))
        .map(row -> row.get("id", UUID.class))
        .one()
        .flatMap(ecritureANouveauxId -> {
          String sqlDetails = """
              INSERT INTO details_ecritures
              (ecriture_id, organization_id, compte_id, libelle, sens, montant_debit, montant_credit, date_ecriture)
              SELECT :ecritureId, :organizationId, compte_id, 'À-nouveaux',
                     CASE WHEN solde >= 0 THEN 'DEBIT' ELSE 'CREDIT' END,
                     CASE WHEN solde >= 0 THEN ABS(solde) ELSE NULL END,
                     CASE WHEN solde < 0 THEN ABS(solde) ELSE NULL END,
                     :dateDebut
              FROM (
                  SELECT compte_id,
                         SUM(COALESCE(montant_debit,0) - COALESCE(montant_credit,0)) as solde
                  FROM details_ecritures
                  WHERE organization_id = :organizationId
                    AND date_ecriture < :dateDebut
                  GROUP BY compte_id
                  HAVING SUM(COALESCE(montant_debit,0) - COALESCE(montant_credit,0)) <> 0
              ) soldes
              """;

          return databaseClient.sql(sqlDetails)
              .bind("ecritureId", ecritureANouveauxId)
              .bind("organizationId", organizationId)
              .bind("dateDebut", dateDebut)
              .then();
        });
  }

  /**
   * Clôture une période comptable avec validation et génération d'écritures.
   */
  @Transactional
  public Mono<Map<String, Object>> cloturerPeriode(UUID periode_id, String user) {
    String sqlPeriode = "SELECT organization_id, date_debut, date_fin FROM periode_comptable WHERE id = :periodeId";

    return databaseClient.sql(sqlPeriode)
        .bind("periodeId", periode_id)
        .fetch()
        .one()
        .flatMap(row -> {
          UUID organization_id = (UUID) row.get("organization_id");
          LocalDate dateFin = (LocalDate) row.get("date_fin");

          return cloturerMoisEtGenererANouveaux(
              organization_id,
              dateFin.getMonthValue(),
              dateFin.getYear())
              .then(Mono.defer(() -> {
                String sqlUpdate = "UPDATE periode_comptable SET cloturee = true, date_cloture = NOW() WHERE id = :periodeId";
                return databaseClient.sql(sqlUpdate)
                    .bind("periodeId", periode_id)
                    .fetch()
                    .rowsUpdated()
                    .map(count -> Map.of(
                        "periode_id", periode_id,
                        "statut", "cloturee",
                        "message", "Période clôturée avec succès"));
              }));
        });
  }

  /**
   * Vérifie si une période est éligible à la clôture.
   */
  public Mono<Map<String, Object>> verifierEligibiliteCloture(UUID periode_id) {
    String sql = """
        SELECT COUNT(*) as ecritures_non_validees
        FROM ecriture_comptable ec
        WHERE ec.periode_id = :periodeId
          AND ec.validee = false
        """;

    return databaseClient.sql(sql)
        .bind("periodeId", periode_id)
        .map(row -> row.get("ecritures_non_validees", Long.class))
        .one()
        .map(count -> {
          boolean eligible = count == 0;
          return Map.of(
              "eligible", eligible,
              "ecritures_non_validees", count,
              "message", eligible ? "Période éligible à la clôture" : count + " écritures non validées");
        });
  }

  /**
   * Annule la clôture d'une période (admin uniquement).
   */
  @Transactional
  public Mono<Void> annulerCloture(UUID periode_id, String user) {
    String sql = "UPDATE periode_comptable SET cloturee = false, date_cloture = NULL WHERE id = :periodeId";
    return databaseClient.sql(sql)
        .bind("periodeId", periode_id)
        .then();
  }
}