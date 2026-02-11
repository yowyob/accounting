package com.yowyob.erp.accounting.util;

import com.yowyob.erp.accounting.entity.FactureComptable;
import com.yowyob.erp.accounting.entity.MouvementStockComptable;
import com.yowyob.erp.accounting.entity.TransactionComptable;
import com.yowyob.erp.common.dto.ComptableObjectRequest;
import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.enums.SourceType;
import com.yowyob.erp.config.organization.OrganizationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Utility class for converting a DTO (ComptableObjectRequest) to a concrete
 * accounting object
 * (Transaction, Invoice, Stock Movement).
 * Follows snake_case naming for variables and matches updated classes.
 *
 * @author ALD
 * @date 12.10.2025
 */
public class AccountingUtils {

    /**
     * Maps a ComptableObjectRequest to a concrete ComptableObject implementation.
     * 
     * @param request the source DTO
     * @return the mapped accounting object
     */
    public static ComptableObject mapToComptableObject(ComptableObjectRequest request) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("Accounting object type is required");
        }

        UUID organization_id = OrganizationContext.getCurrentTenant();
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        UUID journal_comptable_id = request.getJournalComptableId();
        UUID periode_comptable_id = request.getPeriodeComptableId() != null ? request.getPeriodeComptableId()
                : UUID.randomUUID();

        SourceType type = request.getType();

        switch (type) {

            /*
             * ===============================================================
             * 🧾 ACCOUNTING TRANSACTION
             * ===============================================================
             */
            case TRANSACTION -> {
                if (request.getMontant() == null || request.getComptePrincipal() == null
                        || request.getContrepartie() == null || periode_comptable_id == null) {
                    throw new IllegalArgumentException(
                            "Amount, main account, counterpart, and accounting period ID are required for a transaction");
                }

                TransactionComptable transaction = TransactionComptable.builder()
                        .id(request.getId() != null ? request.getId() : UUID.randomUUID())
                        .organization_id(organization_id)
                        .montant(request.getMontant() != null ? request.getMontant() : BigDecimal.ZERO)
                        .date(date)
                        .libelle(request.getLibelle())
                        .journal_comptable_id(journal_comptable_id)
                        .periode_comptable_id(periode_comptable_id)
                        .compte_principal(request.getComptePrincipal())
                        .contrepartie(request.getContrepartie())
                        .build();
                return transaction;
            }

            /*
             * ===============================================================
             * 🧮 ACCOUNTING INVOICE (Purchase or Sale)
             * ===============================================================
             */
            case FACTURE -> {
                if (request.getMontantHT() == null || request.getClientId() == null
                        || periode_comptable_id == null) {
                    throw new IllegalArgumentException(
                            "Amount HT, client ID, and accounting period ID are required for an invoice");
                }

                FactureComptable facture = FactureComptable.builder()
                        .id(request.getId() != null ? request.getId() : UUID.randomUUID())
                        .organization_id(organization_id)
                        .montant_ht(request.getMontantHT())
                        .date(date)
                        .libelle(request.getLibelle())
                        .journal_comptable_id(journal_comptable_id)
                        .periode_comptable_id(periode_comptable_id)
                        .client_id(request.getClientId())
                        .is_achat(Boolean.TRUE.equals(request.getIsAchat()))
                        .build();
                facture.setTenant_id(organization_id);
                return facture;
            }

            /*
             * ===============================================================
             * 📦 STOCK MOVEMENT (Entry or Exit)
             * ===============================================================
             */
            case STOCK -> {
                if (request.getQuantite() == null || request.getCoutUnitaire() == null
                        || periode_comptable_id == null) {
                    throw new IllegalArgumentException(
                            "Quantity, unit cost, and accounting period ID are required for a stock movement");
                }

                MouvementStockComptable mouvement = MouvementStockComptable.builder()
                        .id(request.getId() != null ? request.getId() : UUID.randomUUID())
                        .organization_id(organization_id)
                        .quantite(request.getQuantite())
                        .cout_unitaire(request.getCoutUnitaire())
                        .date(date)
                        .libelle(request.getLibelle())
                        .journal_comptable_id(journal_comptable_id)
                        .periode_comptable_id(periode_comptable_id)
                        .is_entree(Boolean.TRUE.equals(request.getIsEntree()))
                        .fournisseur_id(request.getFournisseurId())
                        .build();
                return mouvement;
            }

            /*
             * ===============================================================
             * ❌ UNSUPPORTED TYPE
             * ===============================================================
             */
            default -> throw new IllegalArgumentException("Unsupported accounting object type: " + type);
        }
    }
}