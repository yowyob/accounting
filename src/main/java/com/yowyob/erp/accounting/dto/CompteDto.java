package com.yowyob.erp.accounting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Accounting Account (Compte).
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompteDto {

    private UUID id;

    @JsonProperty("externalId")
    private UUID external_id;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[1-8][0-9]{4,11}$", message = "Invalid OHADA account number format. Must be between 5 and 12 digits.")
    @JsonProperty("noCompte")
    private String no_compte;

    @NotBlank(message = "Label is required")
    private String libelle;

    private String notes;

    private BigDecimal solde;
    private Integer classe;
    @JsonProperty("typeCompte")
    private String type_compte;

    @Builder.Default
    private Boolean actif = true;

    @JsonProperty("organizationId")
    private UUID organization_id;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("updatedAt")
    private LocalDateTime updated_at;
}