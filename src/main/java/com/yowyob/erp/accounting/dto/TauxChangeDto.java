package com.yowyob.erp.accounting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for TauxChange entity.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TauxChangeDto {

    private UUID id;

    @NotNull(message = "La devise source est obligatoire")
    private UUID devise_source_id;
    private String devise_source_code;

    @NotNull(message = "La devise cible est obligatoire")
    private UUID devise_cible_id;
    private String devise_cible_code;

    @NotNull(message = "Le taux de change est obligatoire")
    private BigDecimal taux;

    @NotNull(message = "La date d'effet est obligatoire")
    private LocalDateTime date_effet;

    private String notes;
    private LocalDateTime created_at;
}
