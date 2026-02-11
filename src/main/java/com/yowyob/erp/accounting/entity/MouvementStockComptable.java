package com.yowyob.erp.accounting.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.enums.SourceType;
import com.yowyob.erp.common.enums.Sens;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an accounting stock movement (entry or exit).
 * Generates a 2-line entry:
 * - Stock account (603000 or 31x depending on case)
 * - Counterpart account (Supplier 401000 or Bank 512000)
 * 
 * Follows snake_case naming for methods as per specific instructions for
 * ComptableObject.
 *
 * @author ALD
 * @date 12.10.2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MouvementStockComptable implements ComptableObject {

    // Account numbers should be injected or looked up, not hardcoded
    // private static final String COMPTE_STOCK = "603000";
    // private static final String COMPTE_FOURNISSEUR = "401000";
    // private static final String COMPTE_BANQUE = "512000";

    private UUID id;
    private UUID tenant_id;
    private int quantite;
    private BigDecimal cout_unitaire; // Updated for financial precision
    private LocalDate date;
    private String libelle;
    private UUID journal_comptable_id;
    private UUID periode_comptable_id;
    private boolean is_entree; // true = entry, false = exit
    private UUID fournisseur_id; // or bank_id depending on case
    private JsonNode attachment_ids;

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
        return cout_unitaire.multiply(BigDecimal.valueOf(quantite));
    }

    @Override
    public LocalDate get_date() {
        return date;
    }

    @Override
    public String get_description() {
        return libelle != null ? libelle : (is_entree ? "Stock entry" : "Stock exit");
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
        return is_entree ? "311000" : "603100"; // Entry: Stock debit, Exit: Expense debit (Variation)
    }

    @Override
    public String get_credit_account() {
        return is_entree ? "401000" : "311000"; // Entry: Supplier credit, Exit: Stock credit
    }

    @Override
    public SourceType get_source_type() {
        return SourceType.STOCK;
    }

    @Override
    public JsonNode get_attachment_ids() {
        return attachment_ids;
    }

    /* Generate accounting lines (2 lines) */
    @Override
    public List<DetailEcriture> generate_ecriture_details(Tenant tenant, EcritureComptable ecriture) {
        List<DetailEcriture> details = new ArrayList<>();
        BigDecimal montant_total = get_montant();
        LocalDateTime now = LocalDateTime.now();

        // Line 1: Stock (603000)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Link to Compte object if needed
                .libelle(is_entree ? "Stock entry" : "Stock exit")
                .sens(is_entree ? Sens.DEBIT : Sens.CREDIT)
                .montant_debit(is_entree ? montant_total : BigDecimal.ZERO)
                .montant_credit(is_entree ? BigDecimal.ZERO : montant_total)
                .date_ecriture(now)
                .created_at(now)
                .updated_at(now)
                .created_by("system")
                .updated_by("system")
                .build());

        // Line 2: Counterpart (Supplier or Bank)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null)
                .libelle(is_entree ? "Supplier" : "Bank")
                .sens(is_entree ? Sens.CREDIT : Sens.DEBIT)
                .montant_credit(is_entree ? montant_total : BigDecimal.ZERO)
                .montant_debit(is_entree ? BigDecimal.ZERO : montant_total)
                .date_ecriture(now)
                .created_at(now)
                .updated_at(now)
                .created_by("system")
                .updated_by("system")
                .build());

        return details;
    }
}