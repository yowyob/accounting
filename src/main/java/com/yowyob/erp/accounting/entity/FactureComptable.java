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
 * Represents an accounting invoice (sale or purchase) according to OHADA
 * standards.
 * Generates a 3-line entry:
 * - Product or expense account (debit or credit)
 * - VAT account (collected or deductible)
 * - Client or supplier account
 * 
 * Follows snake_case naming for methods as per specific instructions for
 * ComptableObject.
 *
 * @author ALD
 * @date 30.09.25
 */
@Data
public class FactureComptable implements ComptableObject {

    private static final BigDecimal TAUX_TVA_DEFAUT = BigDecimal.valueOf(0.18); // Default VAT rate 18% OHADA
    private static final String COMPTE_CHARGE_ACHAT = "601000";
    private static final String COMPTE_CLIENT_VENTE = "411000";
    private static final String COMPTE_FOURNISSEUR_ACHAT = "401000";
    private static final String COMPTE_PRODUIT_VENTE = "701000";

    private UUID id;
    private UUID tenant_id;
    private BigDecimal montant_ht;
    private BigDecimal taux_tva = TAUX_TVA_DEFAUT;
    private LocalDate date;
    private String libelle;
    private UUID journal_comptable_id;
    private UUID periode_comptable_id;
    private UUID client_id;
    private boolean is_achat; // true = purchase, false = sale

    public FactureComptable(UUID id, BigDecimal montant_ht, LocalDate date, String libelle,
            UUID journal_comptable_id, UUID periode_comptable_id, UUID client_id, boolean is_achat) {
        this.id = id;
        this.tenant_id = TenantContext.getCurrentTenant();
        this.montant_ht = montant_ht;
        this.date = date;
        this.libelle = libelle;
        this.journal_comptable_id = journal_comptable_id;
        this.periode_comptable_id = periode_comptable_id;
        this.client_id = client_id;
        this.is_achat = is_achat;
    }

    /* Implementation of ComptableObject */
    @Override
    public UUID get_id() {
        return id;
    }

    @Override
    public UUID get_tenant_id() {
        return tenant_id;
    }

    @Override
    public BigDecimal get_montant() {
        return montant_ht.multiply(BigDecimal.ONE.add(taux_tva)); // TTC
    }

    @Override
    public LocalDate get_date() {
        return date;
    }

    @Override
    public String get_description() {
        return libelle;
    }

    @Override
    public UUID get_journal_comptable_id() {
        return journal_comptable_id;
    }

    @Override
    public UUID get_periode_comptable_id() {
        return periode_comptable_id;
    }

    @Override
    public String get_debit_account() {
        return is_achat ? COMPTE_CHARGE_ACHAT : COMPTE_CLIENT_VENTE;
    }

    @Override
    public String get_credit_account() {
        return is_achat ? COMPTE_FOURNISSEUR_ACHAT : COMPTE_PRODUIT_VENTE;
    }

    @Override
    public SourceType get_source_type() {
        return SourceType.FACTURE;
    }

    /**
     * Generates OHADA accounting lines for the invoice.
     * 
     * @param tenant   the current tenant
     * @param ecriture the associated accounting entry
     * @return list of accounting details
     */
    @Override
    public List<DetailEcriture> generate_ecriture_details(Tenant tenant, EcritureComptable ecriture) {
        List<DetailEcriture> details = new ArrayList<>();
        BigDecimal montant_tva = montant_ht.multiply(taux_tva);
        BigDecimal montant_ttc = montant_ht.add(montant_tva);

        LocalDateTime now = LocalDateTime.now();

        // Line 1: Main account (product or expense)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Should be fetched from repository based on account code
                .libelle(libelle + " - Amount HT")
                .sens(is_achat ? Sens.DEBIT : Sens.CREDIT)
                .montant_debit(is_achat ? montant_ht : BigDecimal.ZERO)
                .montant_credit(is_achat ? BigDecimal.ZERO : montant_ht)
                .date_ecriture(now)
                .created_at(now)
                .updated_at(now)
                .created_by("system")
                .updated_by("system")
                .build());

        // Line 2: VAT (collected or deductible)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Should be fetched from repository
                .libelle(is_achat ? "Deductible VAT" : "Collected VAT")
                .sens(is_achat ? Sens.DEBIT : Sens.CREDIT)
                .montant_debit(is_achat ? montant_tva : BigDecimal.ZERO)
                .montant_credit(is_achat ? BigDecimal.ZERO : montant_tva)
                .date_ecriture(now)
                .created_at(now)
                .updated_at(now)
                .created_by("system")
                .updated_by("system")
                .build());

        // Line 3: Client or supplier
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Should be fetched from repository
                .libelle(is_achat ? "Supplier" : "Client")
                .sens(is_achat ? Sens.CREDIT : Sens.DEBIT)
                .montant_debit(is_achat ? BigDecimal.ZERO : montant_ttc)
                .montant_credit(is_achat ? montant_ttc : BigDecimal.ZERO)
                .date_ecriture(now)
                .created_at(now)
                .updated_at(now)
                .created_by("system")
                .updated_by("system")
                .build());

        return details;
    }
}