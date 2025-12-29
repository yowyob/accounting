package com.yowyob.erp.accounting.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Service for monthly closing and generation of new annual entries.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClotureMensuelleService {

    private final EntityManager em;

    /**
     * Clôture le mois donné et génère les À-nouveaux pour le mois suivant
     * Conforme OHADA : solde des comptes de charges/produits → compte 120 (Résultat)
     * Puis création automatique des écritures À-nouveaux au 01 du mois suivant
     */
    @Transactional
    public void cloturerMoisEtGenererANouveaux(UUID tenantId, int mois, int annee) {
        YearMonth moisCloture = YearMonth.of(annee, mois);
        LocalDate dateCloture = moisCloture.atEndOfMonth();
        LocalDate dateANouveaux = moisCloture.plusMonths(1).atDay(1);

        // Étape 1 : Création de l'écriture de clôture (solde charges/produits → 120)
        UUID ecritureClotureId = creerEcritureCloture(tenantId, dateCloture);

        // Étape 2 : Calcul des soldes des comptes 6 et 7
        List<Object[]> soldes = calculerSoldesClasses67(tenantId, mois, annee);

        BigDecimal totalCharges = BigDecimal.ZERO;
        BigDecimal totalProduits = BigDecimal.ZERO;

        for (Object[] row : soldes) {
            String numeroCompte = (String) row[0];
            BigDecimal solde = (BigDecimal) row[1];

            if (numeroCompte.startsWith("6")) {
                totalCharges = totalCharges.add(solde);
                // Créditer le compte 6
                insererDetailEcriture(ecritureClotureId, tenantId, numeroCompte, solde, "CREDIT");
            } else if (numeroCompte.startsWith("7")) {
                totalProduits = totalProduits.add(solde);
                // Débiter le compte 7
                insererDetailEcriture(ecritureClotureId, tenantId, numeroCompte, solde, "DEBIT");
            }
        }

        // Écriture finale : Report du résultat sur le compte 120
        BigDecimal resultat = totalProduits.subtract(totalCharges);
        String compteResultat = resultat.compareTo(BigDecimal.ZERO) >= 0 ? "120000" : "121000";
        insererDetailEcriture(ecritureClotureId, tenantId, compteResultat, resultat.abs(), resultat.compareTo(BigDecimal.ZERO) >= 0 ? "DEBIT" : "CREDIT");

        // Étape 3 : Génération des À-nouveaux au 01 du mois suivant
        genererEcrituresANouveaux(tenantId, dateANouveaux);
    }

    private UUID creerEcritureCloture(UUID tenantId, LocalDate date) {
        String sql = """
            INSERT INTO ecriture_comptable 
            (tenant_id, date_operation, libelle, journal_code, validee, created_at)
            VALUES (?, ?, 'Clôture mois ' || ? || '/' || ?, 'OD', true, NOW())
            RETURNING id
            """;

        Query q = em.createNativeQuery(sql);
        q.setParameter(1, tenantId);
        q.setParameter(2, date);
        q.setParameter(3, date.getMonthValue());
        q.setParameter(4, date.getYear());
        return (UUID) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> calculerSoldesClasses67(UUID tenantId, int mois, int annee) {
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
            WHERE de.tenant_id = ?
              AND EXTRACT(MONTH FROM de.date_ecriture) = ?
              AND EXTRACT(YEAR FROM de.date_ecriture) = ?
              AND c.numero LIKE '6%' OR c.numero LIKE '7%'
            GROUP BY c.numero
            HAVING SUM(
                     CASE 
                       WHEN de.sens = 'DEBIT' THEN COALESCE(de.montant_debit, 0)
                       ELSE -COALESCE(de.montant_credit, 0)
                     END
                   ) <> 0
            """;

        Query q = em.createNativeQuery(sql);
        q.setParameter(1, tenantId);
        q.setParameter(2, mois);
        q.setParameter(3, annee);
        return q.getResultList();
    }

    private void insererDetailEcriture(UUID ecritureId, UUID tenantId, String numeroCompte, BigDecimal montant, String sens) {
        String sql = """
            INSERT INTO details_ecritures 
            (ecriture_id, tenant_id, compte_id, libelle, sens, montant_debit, montant_credit, date_ecriture, created_at)
            SELECT ?, ?, c.id, 'Clôture automatique', ?, 
                   CASE WHEN ? = 'DEBIT' THEN ? ELSE NULL END,
                   CASE WHEN ? = 'CREDIT' THEN ? ELSE NULL END,
                   NOW(), NOW()
            FROM comptes c 
            WHERE c.tenant_id = ? AND c.numero = ?
            """;

        Query q = em.createNativeQuery(sql);
        q.setParameter(1, ecritureId);
        q.setParameter(2, tenantId);
        q.setParameter(3, sens);
        q.setParameter(4, sens);
        q.setParameter(5, montant);
        q.setParameter(6, montant);
        q.setParameter(7, sens);
        q.setParameter(8, montant);
        q.setParameter(9, tenantId);
        q.setParameter(10, numeroCompte);
        q.executeUpdate();
    }

    private void genererEcrituresANouveaux(UUID tenantId, LocalDate dateDebut) {
        String sql = """
            INSERT INTO ecriture_comptable (tenant_id, date_operation, libelle, journal_code, validee, created_at)
            VALUES (?, ?, 'À-nouveaux', 'AN', true, NOW())
            RETURNING id
            """;

        Query q = em.createNativeQuery(sql);
        q.setParameter(1, tenantId);
        q.setParameter(2, dateDebut);
        UUID ecritureANouveauxId = (UUID) q.getSingleResult();

        // Copie des soldes des comptes de bilan (1 à 5) au 01 du mois
        String sqlDetails = """
            INSERT INTO details_ecritures 
            (ecriture_id, tenant_id, compte_id, libelle, sens, montant_debit, montant_credit, date_ecriture)
            SELECT ?, ?, compte_id, 'À-nouveaux', 
                   CASE WHEN solde >= 0 THEN 'DEBIT' ELSE 'CREDIT' END,
                   CASE WHEN solde >= 0 THEN ABS(solde) ELSE NULL END,
                   CASE WHEN solde < 0 THEN ABS(solde) ELSE NULL END,
                   ?
            FROM (
                SELECT compte_id, 
                       SUM(COALESCE(montant_debit,0) - COALESCE(montant_credit,0)) as solde
                FROM details_ecritures 
                WHERE tenant_id = ? 
                  AND date_ecriture < ?
                GROUP BY compte_id
                HAVING SUM(COALESCE(montant_debit,0) - COALESCE(montant_credit,0)) <> 0
            ) soldes
            """;

        Query q2 = em.createNativeQuery(sqlDetails);
        q2.setParameter(1, ecritureANouveauxId);
        q2.setParameter(2, tenantId);
        q2.setParameter(3, dateDebut);
        q2.setParameter(4, tenantId);
        q2.setParameter(5, dateDebut);
        q2.executeUpdate();
    }
}