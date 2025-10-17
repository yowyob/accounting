package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.enums.SourceType;
import com.yowyob.erp.common.enums.Sens; // Assumez que Sens est un enum partagé
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an accounting stock movement (entry or exit).
 * Generates a 2-line entry:
 *   - Stock account (603000 or 31x depending on case)
 *   - Counterpart account (Supplier 401000 or Bank 512000)
 *
 * @author ALD
 * @date 12/10/2025
 */
@Data
public class MouvementStockComptable implements ComptableObject {

    private static final String COMPTE_STOCK = "603000";
    private static final String COMPTE_FOURNISSEUR = "401000";
    private static final String COMPTE_BANQUE = "512000";

    private UUID id;
    private UUID tenantId;
    private int quantite;
    private BigDecimal coutUnitaire; // Changed to BigDecimal for financial precision
    private LocalDate date;
    private String libelle;
    private UUID journalComptableId;
    private UUID periodeComptableId;
    private boolean isEntree; // true = entry, false = exit
    private UUID fournisseurId; // or bankId depending on case

    public MouvementStockComptable(UUID id, int quantite, BigDecimal coutUnitaire, LocalDate date,
                                   String libelle, UUID journalComptableId,UUID periodeComptableId, boolean isEntree, UUID fournisseurId) {
        this.id = id;
        this.quantite = quantite;
        this.coutUnitaire = coutUnitaire;
        this.date = date;
        this.libelle = libelle;
        this.journalComptableId = journalComptableId;
        this.periodeComptableId = periodeComptableId;
        this.isEntree = isEntree;
        this.fournisseurId = fournisseurId;
    }

    /* Implementation of ComptableObject */
    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    @Override
    public BigDecimal getMontant() {
        return coutUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public String getDescription() {
        return libelle != null ? libelle : (isEntree ? "Stock entry" : "Stock exit");
    }

    @Override
    public UUID getJournalComptableId() {
        return journalComptableId;
    }

    @Override
    public UUID getPeriodeComptableId() {
        return periodeComptableId;
    }   

    @Override
    public String getDebitAccount() {
        // Entry: stock (debit 603000), Exit: charge (debit 603000 also)
        return COMPTE_STOCK; // Stock variation
    }

    @Override
    public String getCreditAccount() {
        // Entry: supplier (401000), Exit: bank (512000)
        return isEntree ? COMPTE_FOURNISSEUR : COMPTE_BANQUE;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.STOCK;
    }

    /* Generate accounting lines (2 lines) */
    @Override
    public List<DetailEcriture> generateEcritureDetails(Tenant tenant, EcritureComptable ecriture) {
        List<DetailEcriture> details = new ArrayList<>();
        BigDecimal montantTotal = getMontant();
        LocalDateTime now = LocalDateTime.now();

        // Line 1: Stock (603000)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Link to Compte object if needed, e.g. fetch from repo
                .libelle(isEntree ? "Stock entry" : "Stock exit")
                .sens(isEntree ? Sens.DEBIT : Sens.CREDIT)
                .montantDebit(isEntree ? montantTotal : BigDecimal.ZERO)
                .montantCredit(isEntree ? BigDecimal.ZERO : montantTotal)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .build());

        // Line 2: Counterpart (Supplier or Bank)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null)
                .libelle(isEntree ? "Supplier" : "Bank")
                .sens(isEntree ? Sens.CREDIT : Sens.DEBIT)
                .montantCredit(isEntree ? montantTotal : BigDecimal.ZERO)
                .montantDebit(isEntree ? BigDecimal.ZERO : montantTotal)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .build());

        return details;
    }
}