package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegleValorisationStockDto {
    private UUID id;

    @NotNull(message = "L'identifiant famille est obligatoire")
    private String familleId;

    @NotNull(message = "Le libellé famille est obligatoire")
    private String familleLibelle;

    @NotNull(message = "La méthode est obligatoire")
    private String methode;

    @NotNull(message = "La date d'application est obligatoire")
    private LocalDate dateApplication;

    @Builder.Default
    private Boolean actif = true;

    private List<HistoriqueValorisationDto> historique;
}
