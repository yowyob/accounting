package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConcordancePeriodeDto {
    private UUID periodeId;
    private List<LigneConcordanceDto> lignesManuelles;
    private ConcordanceCalculDto calcul;
}
