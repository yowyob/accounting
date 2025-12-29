package com.yowyob.erp.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yowyob.erp.common.enums.SourceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Generic DTO to dynamically receive an accounting object
 * (Invoice, Transaction, or Stock Movement) via the REST API.
 * Uses @JsonProperty for snake_case JSON mapping while maintaining camelCase
 * Java fields.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
public class ComptableObjectRequest {

    /*
     * --------------------------------------------------------------------------
     * Accounting object type
     * --------------------------------------------------------------------------
     */
    @NotNull(message = "Accounting object type is required (FACTURE, TRANSACTION, STOCK)")
    private SourceType type;

    /*
     * --------------------------------------------------------------------------
     * Common metadata
     * --------------------------------------------------------------------------
     */
    private UUID id;

    @JsonProperty("tenant_id")
    private UUID tenantId;

    private LocalDate date;
    private String libelle;

    @JsonProperty("journal_comptable_id")
    private UUID journalComptableId;

    @JsonProperty("periode_comptable_id")
    private UUID periodeComptableId;

    /*
     * --------------------------------------------------------------------------
     * Fields for ACCOUNTING TRANSACTIONS
     * --------------------------------------------------------------------------
     */
    private BigDecimal montant;

    @JsonProperty("compte_principal")
    private String comptePrincipal; // e.g., "512000" (bank)

    private String contrepartie; // e.g., "401000" (supplier)

    /*
     * --------------------------------------------------------------------------
     * Fields for ACCOUNTING INVOICES
     * --------------------------------------------------------------------------
     */
    @JsonProperty("montant_ht")
    private BigDecimal montantHT;

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("is_achat")
    private Boolean isAchat; // true = purchase, false = sale

    /*
     * --------------------------------------------------------------------------
     * Fields for STOCK MOVEMENTS
     * --------------------------------------------------------------------------
     */
    private Integer quantite;

    @JsonProperty("cout_unitaire")
    private BigDecimal coutUnitaire;

    @JsonProperty("is_entree")
    private Boolean isEntree; // true = entry, false = exit

    @JsonProperty("fournisseur_id")
    private UUID fournisseurId;
}
