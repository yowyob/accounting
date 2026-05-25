package com.yowyob.erp.accounting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDto {

    private UUID id;

    @NotNull(message = "L'exercice comptable est obligatoire")
    private UUID exerciceId;

    private UUID periodeId;

    private UUID parentId;
    private String parentNom;

    private UUID compteId;
    private String noCompte;
    private String libelleCompte;

    private String code;
    
    @NotNull(message = "Le nom du budget est obligatoire")
    private String nom;

    private BigDecimal montantAlloue;
    private BigDecimal montantConsomme;

    private String libelle;
    private String notes;

    @Builder.Default
    private String type = "EXERCICE"; // EXERCICE | PERIODE | ANALYTIQUE

    @Builder.Default
    private String statut = "BROUILLON"; // BROUILLON | VALIDE | ACTIF | INACTIF | CLOTURE

    @Builder.Default
    private Integer seuilAlerte = 80;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private List<UUID> axeIds;
    private String axeLibelles;

    private List<LigneBudgetCompteDto> compteLines;

    private LocalDateTime createdAt;
    private String createdBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneBudgetCompteDto {
        private UUID id;
        private UUID compteId;
        private String noCompte;
        private String libelleCompte;
        private BigDecimal montantAlloue;
        private BigDecimal montantConsomme;
        private String description;
    }
}
