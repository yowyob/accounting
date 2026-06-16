package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.Data;

/**
 * Selection of components an ADMIN / RESPONSABLE_COMPTABLE wants to initialize for their
 * organization through the onboarding wizard. Every step is idempotent, so the request can be
 * replayed safely. All flags default to {@code true} (initialize everything) to ease first setup.
 */
@Data
public class AccountingSetupRequest {

    /** OHADA chart of accounts (713 accounts). */
    private boolean planComptable = true;

    /** Standard journals: Achats, Ventes, Caisse, Banque, Opérations Diverses. */
    private boolean journaux = true;

    /** Fiscal year for {@link #year} (01/01 → 31/12). */
    private boolean exercice = true;

    /** The 12 monthly accounting periods of {@link #year} (requires/creates the fiscal year). */
    private boolean periodes = true;

    /** Essential ledger accounts + standard operation templates (VENTE/ACHAT/PAIEMENT). */
    private boolean operations = true;

    /** Target fiscal year; defaults to the current calendar year when {@code null}. */
    private Integer year;
}
