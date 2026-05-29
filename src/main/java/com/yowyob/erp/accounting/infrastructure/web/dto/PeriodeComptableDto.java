package com.yowyob.erp.accounting.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Accounting Period (Periode Comptable).
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodeComptableDto {

    private UUID id;

    @JsonProperty("exercice_id")
    private UUID exercice_id;

    @NotBlank(message = "Le code ne peut pas être vide")
    @Size(max = 50, message = "Le code ne doit pas dépasser 50 caractères")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Le code doit être au format YYYY-MM")
    private String code;

    @NotNull(message = "La date de début ne peut pas être nulle")
    @JsonProperty("dateDebut")
    private LocalDate date_debut;

    @NotNull(message = "La date de fin ne peut pas être nulle")
    @JsonProperty("dateFin")
    private LocalDate date_fin;

    @NotNull(message = "Le statut clôturé ne peut pas être nul")
    private Boolean cloturee;

    @JsonProperty("dateCloture")
    private LocalDate date_cloture;

    @Size(max = 255, message = "Les notes ne doivent pas dépasser 255 caractères")
    private String notes;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("updatedAt")
    private LocalDateTime updated_at;

    @JsonProperty("createdBy")
    private String created_by;

    @JsonProperty("updatedBy")
    private String updated_by;
}