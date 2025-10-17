package com.yowyob.erp.common.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.common.enums.SourceType;

/**
 * Interface générique représentant tout objet comptable :
 * Transaction, Facture, Mouvement de stock, etc.
 *
 * Elle permet une intégration uniforme dans le module de comptabilité.
 */
public interface ComptableObject {

    /** Identifiant unique de l’objet (facture, transaction, etc.) */
    UUID getId();

    /** Identifiant du tenant (multi-tenant) */
    UUID getTenantId();

    /** Montant total de l’opération */
    BigDecimal getMontant();

    /** Date de l’opération */
    LocalDate getDate();

    /** Libellé ou description */
    String getDescription();

    /** Journal comptable associé */
    UUID getJournalComptableId();

    /**Periode comptable associé */
    UUID getPeriodeComptableId();

    /** Compte de débit (ex: 411000 pour client, 512000 pour banque) */
    String getDebitAccount();

    /** Compte de crédit (ex: 707000 pour ventes, 445700 pour TVA collectée) */
    String getCreditAccount();

    /** Source de l’objet (FACTURE, TRANSACTION, STOCK, etc.) */
    SourceType getSourceType();

    /**
     * Génère les lignes de détail d’écriture associées à cet objet.
     * Utilise les relations avec Tenant et EcritureComptable pour maintenir l'intégrité.
     */
    List<DetailEcriture> generateEcritureDetails(Tenant tenant, EcritureComptable ecriture);
}