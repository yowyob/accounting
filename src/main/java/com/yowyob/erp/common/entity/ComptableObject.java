package com.yowyob.erp.common.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.common.enums.SourceType;

/**
 * Generic interface representing any accounting object:
 * Transaction, Invoice, Stock movement, etc.
 * 
 * Allows uniform integration into the accounting module.
 * Follows snake_case naming for methods as per specific instructions.
 * 
 * @author ALD
 * @date 30.09.25
 */
public interface ComptableObject {

    /**
     * Unique identifier of the object (invoice, transaction, etc.).
     * 
     * @return the unique ID
     */
    UUID get_id();

    /**
     * Identifier of the tenant (multi-tenancy).
     * 
     * @return the tenant ID
     */
    UUID get_organization_id();

    /**
     * Total amount of the operation.
     * 
     * @return the amount
     */
    BigDecimal get_montant();

    /**
     * Date of the operation.
     * 
     * @return the operation date
     */
    LocalDate get_date();

    /**
     * Label or description.
     * 
     * @return the description
     */
    String get_description();

    /**
     * Associated accounting journal ID.
     * 
     * @return the journal ID
     */
    UUID get_journal_comptable_id();

    /**
     * Associated accounting period ID.
     * 
     * @return the period ID
     */
    UUID get_periode_comptable_id();

    /**
     * Debit account (e.g., 411000 for customer, 512000 for bank).
     * 
     * @return the debit account number
     */
    String get_debit_account();

    /**
     * Credit account (e.g., 707000 for sales, 445700 for collected VAT).
     * 
     * @return the credit account number
     */
    String get_credit_account();

    /**
     * Source of the object (INVOICE, TRANSACTION, STOCK, etc.).
     * 
     * @return the source type
     */
    SourceType get_source_type();

    /**
     * Proof or attachments.
     * 
     * @return the attachments as JSON
     */
    default JsonNode get_attachment_ids() {
        return null;
    }

    /**
     * Generates the accounting entry detail lines associated with this object.
     * 
     * @param tenant   the current tenant
     * @param ecriture the accounting entry
     * @return the list of entry details
     */
    List<DetailEcriture> generate_ecriture_details(Organization tenant, EcritureComptable ecriture);
}