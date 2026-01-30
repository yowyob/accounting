package com.yowyob.erp.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Taxe entity.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxeDto {

    private UUID id;

    @NotBlank(message = "Le code de la taxe est obligatoire")
    private String code;

    @NotBlank(message = "Le libelle de la taxe est obligatoire")
    private String libelle;

    @NotNull(message = "Le taux est obligatoire")
    @Positive(message = "Le taux doit être positif")
    private BigDecimal taux;

    private String type_taxe;

    private String compte_collecte;
    private String compte_deductible;
    private String pays;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date_debut_validite;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date_fin_validite;
    private boolean actif;
    private LocalDateTime created_at;
}
