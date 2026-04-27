package com.yowyob.erp.accounting.entity;

/**
 * Types de régularisations de fin de période OHADA.
 *
 * CCA : Charges Constatées d'Avance  → compte 476
 *       Écriture : Débit 476 / Crédit 6xx
 *       Extourne : Débit 6xx / Crédit 476
 *
 * PCA : Produits Constatés d'Avance  → compte 477
 *       Écriture : Débit 7xx / Crédit 477
 *       Extourne : Débit 477 / Crédit 7xx
 *
 * CAP : Charges À Payer              → compte 408/428/448
 *       Écriture : Débit 6xx / Crédit 408|428|448
 *       Extourne : Débit 408|428|448 / Crédit 6xx
 *
 * PAR : Produits À Recevoir          → compte 418/438
 *       Écriture : Débit 418|438 / Crédit 7xx
 *       Extourne : Débit 7xx / Crédit 418|438
 */
public enum TypeRegularisation {

    /** Charges Constatées d'Avance — compte OHADA 476 */
    CCA("476", "Charges Constatées d'Avance"),

    /** Produits Constatés d'Avance — compte OHADA 477 */
    PCA("477", "Produits Constatés d'Avance"),

    /** Charges À Payer — comptes OHADA 408 (fournisseurs), 428 (personnel), 448 (État) */
    CAP("408", "Charges À Payer"),

    /** Produits À Recevoir — comptes OHADA 418 (clients), 438 (organismes sociaux) */
    PAR("418", "Produits À Recevoir");

    private final String compteOhadaParDefaut;
    private final String libelle;

    TypeRegularisation(String compteOhadaParDefaut, String libelle) {
        this.compteOhadaParDefaut = compteOhadaParDefaut;
        this.libelle = libelle;
    }

    public String getCompteOhadaParDefaut() {
        return compteOhadaParDefaut;
    }

    public String getLibelle() {
        return libelle;
    }
}
