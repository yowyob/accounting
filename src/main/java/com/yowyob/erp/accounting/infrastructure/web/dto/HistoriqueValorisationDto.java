package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;

import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HistoriqueValorisationDto {
    private String methode;
    private LocalDate du;
    private LocalDate au;
}
