package com.yowyob.erp.accounting.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BilanDto {
    private List<ReportItemDto> actifs;
    private List<ReportItemDto> passifs;
    private List<ReportItemDto> capitauxPropres;
}
