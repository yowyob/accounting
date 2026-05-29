package com.yowyob.erp.shared.domain.enums;

import lombok.Getter;

@Getter
public enum OHADAAccountClass {
    CLASS_1("1", "Ressources durables", "Passif"),
    CLASS_2("2", "Immobilisations", "Actif"),
    CLASS_3("3", "Stocks", "Actif"),
    CLASS_4("4", "Tiers", "Actif/Passif"),
    CLASS_5("5", "Trésorerie", "Actif/Passif"),
    CLASS_6("6", "Charges", "Résultat"),
    CLASS_7("7", "Produits", "Résultat"),
    CLASS_8("8", "Hors activités ordinaires", "Spécial"),
    CLASS_9("9", "Comptabilité analytique", "Analytique");

    private final String code;
    private final String description;
    private final String type;

    OHADAAccountClass(String code, String description, String type) {
        this.code = code;
        this.description = description;
        this.type = type;
    }
}