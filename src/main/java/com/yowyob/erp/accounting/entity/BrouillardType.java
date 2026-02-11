package com.yowyob.erp.accounting.entity;

/**
 * Types d'objets pouvant passer par le brouillard comptable.
 */
public enum BrouillardType {
    FACTURE_CLIENT,
    FACTURE_FOURNISSEUR,
    MOUVEMENT_STOCK,
    MOUVEMENT_CAISSE,
    OPERATION_BANCAIRE,
    AUTRE
}
