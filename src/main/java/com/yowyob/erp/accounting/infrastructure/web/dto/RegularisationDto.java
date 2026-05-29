package com.yowyob.erp.accounting.infrastructure.web.dto;

import com.yowyob.erp.accounting.domain.model.StatutRegularisation;
import com.yowyob.erp.accounting.domain.model.TypeRegularisation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegularisationDto {

    private UUID id;

    @NotNull(message = "Le type de régularisation est obligatoire (CCA, PCA, CAP, PAR)")
    private TypeRegularisation typeRegularisation;

    private StatutRegularisation statut;

    @NotNull(message = "La période comptable est obligatoire")
    private UUID periodeId;

    @NotNull(message = "La date de régularisation est obligatoire")
    private LocalDate dateRegularisation;

    @NotNull(message = "Le compte de charge/produit est obligatoire")
    private UUID compteChargeProduitId;

    @NotNull(message = "Le compte de régularisation OHADA est obligatoire (476, 477, 408, 418...)")
    private UUID compteRegularisationId;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montant;

    @NotNull(message = "Le libellé est obligatoire")
    private String libelle;

    private String notes;

    // Champs calculés (lecture seule)
    private UUID ecritureInitialeId;
    private LocalDate dateExtourne;
    private UUID ecritureExtourneId;
    private String extourneePar;
    private LocalDateTime dateExtourneEffective;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
