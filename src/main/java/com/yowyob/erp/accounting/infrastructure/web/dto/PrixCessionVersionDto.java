package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrixCessionVersionDto {
    private UUID id;
    private BigDecimal prixUnitaire;
    private String methode;
    private LocalDate du;
    private LocalDate au;
}
