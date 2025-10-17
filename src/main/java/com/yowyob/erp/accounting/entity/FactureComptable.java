package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.enums.SourceType;
import com.yowyob.erp.config.tenant.TenantContext;
import com.yowyob.erp.common.enums.Sens;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an accounting invoice (sale or purchase) according to OHADA standards.
 * Generates a 3-line entry:
 *   - Product or expense account (debit or credit)
 *   - VAT account (collected or deductible)
 *   - Client or supplier account
 *
 * @author ALD
 * @date 12/10/2025 07:51 AM WAT
 */
@Data
public class FactureComptable implements ComptableObject {

    private static final BigDecimal TAUX_TVA_DEFAUT = BigDecimal.valueOf(0.18); // Default VAT rate 18% OHADA
    private static final String COMPTE_CHARGE_ACHAT = "601000";
    private static final String COMPTE_CLIENT_VENTE = "411000";
    private static final String COMPTE_FOURNISSEUR_ACHAT = "401000";
    private static final String COMPTE_PRODUIT_VENTE = "701000";
    private static final String COMPTE_TVA_DEDUCTIBLE = "445100"; // Example for deductible VAT
    private static final String COMPTE_TVA_COLLECTEE = "445700"; // Example for collected VAT

    private UUID id;
    private UUID tenantId;
    private BigDecimal montantHT; // Changed to BigDecimal for financial precision
    private BigDecimal tauxTVA = TAUX_TVA_DEFAUT;
    private LocalDate date;
    private String libelle;
    private UUID journalComptableId;
    private UUID periodeComptableId;
    private UUID clientId; // or supplierId depending on case
    private boolean isAchat; // true = purchase, false = sale

    public FactureComptable(UUID id, BigDecimal montantHT, LocalDate date, String libelle,
                            UUID journalComptableId, UUID periodeComptableId, UUID clientId, boolean isAchat) {
        this.id = id;
        this.tenantId = TenantContext.getCurrentTenant(); 
        this.montantHT = montantHT;
        this.date = date;
        this.libelle = libelle;
        this.journalComptableId = journalComptableId;
        this.periodeComptableId = periodeComptableId;
        this.clientId = clientId;
        this.isAchat = isAchat;
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
        return montantHT.multiply(BigDecimal.ONE.add(tauxTVA)); // TTC
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public String getDescription() {
        return libelle;
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
        return isAchat ? COMPTE_CHARGE_ACHAT : COMPTE_CLIENT_VENTE;
    }

    @Override
    public String getCreditAccount() {
        return isAchat ? COMPTE_FOURNISSEUR_ACHAT : COMPTE_PRODUIT_VENTE;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.FACTURE;
    }

    /* Generate OHADA accounting lines */
    @Override
    public List<DetailEcriture> generateEcritureDetails(Tenant tenant, EcritureComptable ecriture) {
        List<DetailEcriture> details = new ArrayList<>();
        BigDecimal montantTVA = montantHT.multiply(tauxTVA);
        BigDecimal montantTTC = montantHT.add(montantTVA);

        LocalDateTime now = LocalDateTime.now();

        // Line 1: Main account (product or expense)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Should be fetched from repository based on account code
                .libelle(libelle + " - Amount HT")
                .sens(isAchat ? Sens.DEBIT : Sens.CREDIT)
                .montantDebit(isAchat ? montantHT : BigDecimal.ZERO)
                .montantCredit(isAchat ? BigDecimal.ZERO : montantHT)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .build());

        // Line 2: VAT (collected or deductible)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Should be fetched from repository
                .libelle(isAchat ? "Deductible VAT" : "Collected VAT")
                .sens(isAchat ? Sens.DEBIT : Sens.CREDIT)
                .montantDebit(isAchat ? montantTVA : BigDecimal.ZERO)
                .montantCredit(isAchat ? BigDecimal.ZERO : montantTVA)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .build());

        // Line 3: Client or supplier
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Should be fetched from repository
                .libelle(isAchat ? "Supplier" : "Client")
                .sens(isAchat ? Sens.CREDIT : Sens.DEBIT)
                .montantDebit(isAchat ? BigDecimal.ZERO : montantTTC)
                .montantCredit(isAchat ? montantTTC : BigDecimal.ZERO)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .build());

        return details;
    }
}