package com.yowyob.erp.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for ExerciceComptable.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciceComptableDto {
    private UUID id;
    private UUID tenant_id;
    private String code;
    private String libelle;
    private LocalDate date_debut;
    private LocalDate date_fin;
    private Boolean cloture;
    private String statut;
    private Boolean actif;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private String created_by;
}
