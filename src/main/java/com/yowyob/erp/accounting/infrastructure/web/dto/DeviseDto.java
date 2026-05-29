package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Devise entity.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviseDto {

    private UUID id;

    @NotBlank(message = "Le code de la devise est obligatoire")
    private String code;

    @NotBlank(message = "Le nom de la devise est obligatoire")
    private String nom;

    private String symbole;
    private boolean est_nationale;
    private boolean actif;
    private LocalDateTime created_at;
}
