package com.yowyob.erp.accounting.domain.model;

public enum StatutRegularisation {
    /** Régularisation enregistrée, en attente d'extourne */
    ACTIVE,
    /** Extourne générée automatiquement au début de la période suivante */
    EXTOURNEE,
    /** Annulée manuellement avant extourne */
    ANNULEE
}
