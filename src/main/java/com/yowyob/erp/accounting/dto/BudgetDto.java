package com.yowyob.erp.accounting.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDto {

    private UUID id;

    @NotNull(message = "L'exercice comptable est obligatoire")
    private UUID exerciceId;

    private UUID periodeId;

    @NotNull(message = "Le compte est obligatoire")
    private UUID compteId;

    private String noCompte;
    private String libelleCompte;

    @NotNull(message = "Le montant budgété est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montantBudget;

    private String libelle;
    private String notes;

    /** PREVISIONNEL | REVISE */
    @Builder.Default
    private String type = "PREVISIONNEL";

    private LocalDateTime createdAt;
    private String createdBy;
}
