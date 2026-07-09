package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VentilationAxeDto {
    private UUID id;
    private UUID axeId;
    private String axeLibelle;
    private UUID centreId;
    private String centreLibelle;

    @NotNull(message = "Le pourcentage est obligatoire")
    private BigDecimal pourcentage;
}
