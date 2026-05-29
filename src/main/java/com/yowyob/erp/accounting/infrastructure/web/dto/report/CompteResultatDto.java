package com.yowyob.erp.accounting.infrastructure.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompteResultatDto {
    private List<ReportItemDto> produits;
    private List<ReportItemDto> charges;
}
