package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LigneConcordanceDto {
    private UUID id;

    @NotNull(message = "Le type est obligatoire")
    private String type;

    @NotNull(message = "Le libellé est obligatoire")
    private String label;

    private String description;

    @NotNull(message = "Le signe est obligatoire")
    private String signe;

    @NotNull(message = "Le montant est obligatoire")
    private BigDecimal montant;

    private UUID chargeVentileeId;

    @Builder.Default
    private Boolean autoGeneree = false;
}
