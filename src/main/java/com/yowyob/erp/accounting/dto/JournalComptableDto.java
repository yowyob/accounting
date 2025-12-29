package com.yowyob.erp.accounting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Accounting Journal (Journal Comptable).
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalComptableDto {

    private UUID id;

    @NotBlank(message = "Journal code is required")
    @JsonProperty("codeJournal")
    private String code_journal;

    @NotBlank(message = "Label is required")
    private String libelle;

    @NotBlank(message = "Journal type is required")
    @JsonProperty("typeJournal")
    private String type_journal;

    private String notes;

    @Builder.Default
    private Boolean actif = true;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("updatedAt")
    private LocalDateTime updated_at;

    // List of accounting entries
    @JsonProperty("ecritureComptable")
    private List<EcritureComptableDto> ecriture_comptable;
}