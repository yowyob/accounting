package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalAnalytiqueDto {
    private UUID id;

    @NotNull(message = "Le code est obligatoire")
    private String code;

    @NotNull(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "Le type est obligatoire")
    private String type; // CHARGES, PRODUITS, REPARTITION, CORRECTION

    @Builder.Default
    private Boolean actif = true;

    private LocalDateTime updatedAt;
}
