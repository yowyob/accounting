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
 * Represents an accounting transaction (payment, receipt, transfer, etc.)
 * and automatically generates a two-line entry:
 *   - Debit on the main account (e.g., Bank)
 *   - Credit on the counterpart account (e.g., Supplier, Client)
 *
 * @author ALD
 * @date 12/10/2025
 */
@Data
public class TransactionComptable implements ComptableObject {

    private UUID id;
    private UUID tenantId;
    private BigDecimal montant;
    private LocalDate date;
    private String libelle;
    private UUID journalComptableId;
    private UUID periodeComptableId;    

    // Symbolic representation (noCompte) — more practical for generation
    private String comptePrincipal; // e.g., "512000" Bank
    private String contrepartie;    // e.g., "401000" Supplier

    public TransactionComptable(UUID id, BigDecimal montant, LocalDate date, String libelle,
                                UUID journalComptableId,UUID periodeComptableId, String comptePrincipal, String contrepartie) {
        this.id = id;
        this.montant = montant;
        this.date = date;
        this.libelle = libelle;
        this.journalComptableId = journalComptableId;
        this.periodeComptableId = periodeComptableId;
        this.comptePrincipal = comptePrincipal;
        this.contrepartie = contrepartie;
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
        return montant;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public String getDescription() {
        return libelle != null ? libelle : "Accounting transaction";
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
        return comptePrincipal; // e.g., Bank 512000
    }

    @Override
    public String getCreditAccount() {
        return contrepartie; // e.g., Supplier 401000
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.TRANSACTION;
    }

    /* Automatically generate accounting entries (2 lines) */
    @Override
    public List<DetailEcriture> generateEcritureDetails(Tenant tenant, EcritureComptable ecriture) {
        List<DetailEcriture> details = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Line 1: Debit (main account)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Link to Compte object if needed, e.g., fetch from repo
                .libelle("Debit: " + libelle)
                .sens(Sens.DEBIT)
                .montantDebit(montant) 
                .montantCredit(BigDecimal.ZERO)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .build());

        // Line 2: Credit (counterpart)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null)
                .libelle("Credit: " + libelle)
                .sens(Sens.CREDIT)
                .montantCredit(montant) 
                .montantDebit(BigDecimal.ZERO)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .build());

        return details;
    }
}