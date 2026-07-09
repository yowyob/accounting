package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MethodeCalculCoutDto {
    private UUID id;

    @NotNull(message = "La méthode est obligatoire")
    private String methode;

    @NotNull(message = "Le plan analytique est obligatoire")
    private String planAnalytiqueId;

    @NotNull(message = "La date d'application est obligatoire")
    private LocalDate dateApplication;

    @NotNull(message = "Le statut est obligatoire")
    private String statut;

    private String description;

    private List<ActiviteNormaleDto> activitesNormales;
}
