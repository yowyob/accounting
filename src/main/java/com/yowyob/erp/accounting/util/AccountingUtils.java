package com.yowyob.erp.accounting.util;

import com.yowyob.erp.accounting.entity.FactureComptable;
import com.yowyob.erp.accounting.entity.MouvementStockComptable;
import com.yowyob.erp.accounting.entity.TransactionComptable;
import com.yowyob.erp.common.dto.ComptableObjectRequest;
import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.enums.SourceType;
import com.yowyob.erp.config.tenant.TenantContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Utility class for converting a DTO (ComptableObjectRequest) to a concrete accounting object
 * (Transaction, Invoice, Stock Movement).
 *
 * @author ALD
 * @date 12/10/2025 04:08 PM WAT
 */
public class AccountingUtils {

    public static ComptableObject mapToComptableObject(ComptableObjectRequest request) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("Accounting object type is required");
        }

        UUID tenantId = TenantContext.getCurrentTenant();// Use TenantContext for consistency
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        UUID journalComptableId = request.getJournalComptableId();
        UUID periodeComptableId = request.getPeriodeComptableId() != null ? request.getPeriodeComptableId() : UUID.randomUUID();

        SourceType type = request.getType();

        switch (type) {

            /* ===============================================================
             * 🧾 ACCOUNTING TRANSACTION
             * =============================================================== */
            case TRANSACTION -> {
                if (request.getMontant() == null || request.getComptePrincipal() == null || request.getContrepartie() == null || periodeComptableId == null) {
                    throw new IllegalArgumentException("Montant, compte principal, contrepartie, and periodeComptableId are required for a transaction");
                }

                TransactionComptable transaction = new TransactionComptable(
                        request.getId() != null ? request.getId() : UUID.randomUUID(),
                        request.getMontant() != null ? request.getMontant() : BigDecimal.ZERO,
                        date,
                        request.getLibelle(),
                        journalComptableId,
                        periodeComptableId,
                        request.getComptePrincipal(),
                        request.getContrepartie()
                );
                transaction.setTenantId(tenantId);
                return transaction;
            }

            /* ===============================================================
             * 🧮 ACCOUNTING INVOICE (Purchase or Sale)
             * =============================================================== */
            case FACTURE -> {
                if (request.getMontantHT() == null || request.getClientId() == null || periodeComptableId == null) {
                    throw new IllegalArgumentException("Montant HT, clientId, and periodeComptableId are required for an invoice");
                }

                FactureComptable facture = new FactureComptable(
                        request.getId() != null ? request.getId() : UUID.randomUUID(),
                        request.getMontantHT(),
                        date,
                        request.getLibelle(),
                        journalComptableId,
                        periodeComptableId,
                        request.getClientId(),
                        Boolean.TRUE.equals(request.getIsAchat())
                );
                facture.setTenantId(tenantId);
                return facture;
            }

            /* ===============================================================
             * 📦 STOCK MOVEMENT (Entry or Exit)
             * =============================================================== */
            case STOCK -> {
                if (request.getQuantite() == null || request.getCoutUnitaire() == null || periodeComptableId == null) {
                    throw new IllegalArgumentException("Quantite, cout unitaire, and periodeComptableId are required for a stock movement");
                }

                MouvementStockComptable mouvement = new MouvementStockComptable(
                        request.getId() != null ? request.getId() : UUID.randomUUID(),
                        request.getQuantite(),
                        request.getCoutUnitaire(),
                        date,
                        request.getLibelle(),
                        journalComptableId,
                        periodeComptableId,
                        Boolean.TRUE.equals(request.getIsEntree()),
                        request.getFournisseurId()
                );
                mouvement.setTenantId(tenantId);
                return mouvement;
            }

            /* ===============================================================
             * ❌ UNSUPPORTED TYPE
             * =============================================================== */
            default -> throw new IllegalArgumentException("Unsupported accounting object type: " + type);
        }
    }
}