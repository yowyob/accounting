package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChargeVentileeDto {
    private UUID id;
    private String chargeSourceId;

    @NotNull(message = "Le compte CG est obligatoire")
    private String compteCG;

    @NotNull(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "Le montant total est obligatoire")
    private BigDecimal montantTotal;

    @Builder.Default
    private Boolean incorporable = true;

    @NotNull(message = "La période est obligatoire")
    private UUID periodeId;

    private UUID periodeCgId;

    private List<VentilationAxeDto> ventilations;
}
