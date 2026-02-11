package com.yowyob.erp.accounting.entity;

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
 * Represents an accounting transaction (payment, receipt, transfer, etc.)
 * and automatically generates a two-line entry:
 * - Debit on the main account (e.g., Bank)
 * - Credit on the counterpart account (e.g., Supplier, Client)
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
public class TransactionComptable implements ComptableObject {

    private UUID id;
    private UUID organization_id;
    private BigDecimal montant;
    private LocalDate date;
    private String libelle;
    private UUID journal_comptable_id;
    private UUID periode_comptable_id;

    // Symbolic representation (no_compte) — more practical for generation
    private String compte_principal; // e.g., "512000" Bank
    private String contrepartie; // e.g., "401000" Supplier


    /* Implementation of ComptableObject */
    @Override
    public UUID get_id() {
        return id;
    }

    @Override
    public UUID get_organization_id() {
        return organization_id;
    }

    @Override
    public BigDecimal get_montant() {
        return montant;
    }

    @Override
    public LocalDate get_date() {
        return date;
    }

    @Override
    public String get_description() {
        return libelle != null ? libelle : "Accounting transaction";
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
        return compte_principal; // e.g., Bank 512000
    }

    @Override
    public String get_credit_account() {
        return contrepartie; // e.g., Supplier 401000
    }

    @Override
    public SourceType get_source_type() {
        return SourceType.TRANSACTION;
    }

    /* Automatically generate accounting entries (2 lines) */
    @Override
    public List<DetailEcriture> generate_ecriture_details(Organization tenant, EcritureComptable ecriture) {
        List<DetailEcriture> details = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Line 1: Debit (main account)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null) // Link to Compte object if needed
                .libelle("Debit: " + libelle)
                .sens(Sens.DEBIT)
                .montant_debit(montant)
                .montant_credit(BigDecimal.ZERO)
                .date_ecriture(now)
                .created_at(now)
                .updated_at(now)
                .created_by("system")
                .updated_by("system")
                .build());

        // Line 2: Credit (counterpart)
        details.add(DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(null)
                .libelle("Credit: " + libelle)
                .sens(Sens.CREDIT)
                .montant_credit(montant)
                .montant_debit(BigDecimal.ZERO)
                .date_ecriture(now)
                .created_at(now)
                .updated_at(now)
                .created_by("system")
                .updated_by("system")
                .build());

        return details;
    }
}