package com.yowyob.erp.accounting.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Plan Comptable Template.
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanComptableTemplateDto {

    private UUID id;

    @NotBlank(message = "The account number is required")
    @Pattern(regexp = "^[1-8][0-9]{4,7}$", message = "Invalid OHADA account number format")
    private String numero;

    @NotBlank(message = "The label is required")
    private String libelle;

    private Integer classe;

    private String notes;

    @Builder.Default
    private Boolean actif = true;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("updatedAt")
    private LocalDateTime updated_at;

    @JsonProperty("createdBy")
    private String created_by;

    @JsonProperty("updatedBy")
    private String updated_by;
}