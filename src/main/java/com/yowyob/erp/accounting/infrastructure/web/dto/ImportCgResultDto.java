package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportCgResultDto {
    @Builder.Default
    private List<EcritureAnalytiqueDto> created = new ArrayList<>();
    @Builder.Default
    private int ignored = 0;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
