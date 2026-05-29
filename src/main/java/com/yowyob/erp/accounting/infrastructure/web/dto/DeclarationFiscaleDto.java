package com.yowyob.erp.accounting.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for DeclarationFiscale.
 * Used for exchanging tax declaration information via the API.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclarationFiscaleDto {

    private UUID id;

    @NotBlank(message = "Declaration type is required")
    @JsonProperty("typeDeclaration")
    private String type_declaration;

    @NotNull(message = "Start period is required")
    @JsonProperty("periodeDebut")
    private LocalDate periode_debut;

    @NotNull(message = "End period is required")
    @JsonProperty("periodeFin")
    private LocalDate periode_fin;

    @NotNull(message = "Total amount is required")
    @JsonProperty("montantTotal")
    private Double montant_total;

    @NotNull(message = "Generation date is required")
    @JsonProperty("dateGeneration")
    private LocalDate date_generation;

    @NotBlank(message = "Status is required")
    private String statut;

    @JsonProperty("numeroDeclaration")
    private String numero_declaration;

    @JsonProperty("donneesDeclaration")
    private String donnees_declaration;

    private String notes;
}