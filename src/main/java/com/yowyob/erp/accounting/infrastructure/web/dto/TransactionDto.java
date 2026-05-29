// DTO pour les transactions
package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private Long id;

    private String numeroRecu;

    private Long operationComptableId;

    @NotNull(message = "Le montant de la transaction est obligatoire")
    private Double montantTransaction;

    private String montantLettre;

    @Builder.Default
    private Boolean estMontantTTC = true;

    @NotNull(message = "La date de transaction est obligatoire")
    private LocalDateTime dateTransaction;

    @Builder.Default
    private Boolean estValidee = false;

    private LocalDateTime dateValidation;

    private String referenceObjet;

    private String caissier;

    @Builder.Default
    private Boolean estComptabilisee = false;

    private String notes;

    private Long ecritureComptableId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
