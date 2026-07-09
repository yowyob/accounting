package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PeriodeAnalytiqueDto {
    private UUID id;

    @NotNull(message = "L'exercice comptable est obligatoire")
    private UUID exerciceId;

    @NotNull(message = "Le code est obligatoire")
    private String code;

    @NotNull(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    @Builder.Default
    private String statut = "OUVERTE"; // OUVERTE, CLOTUREE
}
