package com.yowyob.erp.common.dto;

import com.yowyob.erp.common.enums.SourceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO générique permettant de recevoir dynamiquement un objet comptable
 * (Facture, Transaction ou Mouvement de Stock) via l’API REST.
 */
@Data
public class ComptableObjectRequest {

    /* --------------------------------------------------------------------------
     * Type d’objet comptable
     * -------------------------------------------------------------------------- */
    @NotNull(message = "Le type de l'objet comptable est requis (FACTURE, TRANSACTION, STOCK)")
    private SourceType type;

    /* --------------------------------------------------------------------------
     * Métadonnées communes
     * -------------------------------------------------------------------------- */
    private UUID id;
    private UUID tenantId;
    private LocalDate date;
    private String libelle;
    private UUID journalComptableId;
    private UUID periodeComptableId;

    /* --------------------------------------------------------------------------
     * Champs pour les TRANSACTIONS COMPTABLES
     * -------------------------------------------------------------------------- */
    private BigDecimal montant;
    private String comptePrincipal; // ex: "512000" (banque)
    private String contrepartie;    // ex: "401000" (fournisseur)

    /* --------------------------------------------------------------------------
     * Champs pour les FACTURES COMPTABLES
     * -------------------------------------------------------------------------- */
    private BigDecimal montantHT;
    private UUID clientId;
    private Boolean isAchat; // true = achat, false = vente

    /* --------------------------------------------------------------------------
     * Champs pour les MOUVEMENTS DE STOCK
     * -------------------------------------------------------------------------- */
    private Integer quantite;
    private BigDecimal coutUnitaire;
    private Boolean isEntree; // true = entrée, false = sortie
    private UUID fournisseurId;
}
