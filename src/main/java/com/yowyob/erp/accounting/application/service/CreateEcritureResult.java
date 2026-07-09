package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureAnalytiqueDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateEcritureResult {
    private final EcritureAnalytiqueDto dto;
    private final boolean alreadyProcessed;
}
