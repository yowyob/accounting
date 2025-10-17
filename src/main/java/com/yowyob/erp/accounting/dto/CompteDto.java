package com.yowyob.erp.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompteDto {

    private UUID id;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[1-8][0-9]{4,7}$", message = "Invalid OHADA account number format")
    private String noCompte; 

    @NotBlank(message = "Label is required")
    private String libelle;

    private String notes;

    private BigDecimal solde;
    private Integer classe;
    private String typeCompte;

    @Builder.Default
    private Boolean actif = true;

    private UUID tenantId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}