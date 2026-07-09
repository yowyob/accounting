package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureComptableDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateEcritureComptableResult {
    private final EcritureComptableDto dto;
    private final boolean alreadyProcessed;
}
