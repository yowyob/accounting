package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.domain.model.Taxe;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.TaxeDto;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the Taxe operations.
 */
public interface TaxeUseCase {
    Mono<TaxeDto> createTaxe(TaxeDto dto);
    Mono<TaxeDto> updateTaxe(UUID id, TaxeDto dto);
    Mono<TaxeDto> getTaxe(UUID id);
    Mono<List<TaxeDto>> getAllTaxes();
    Mono<List<TaxeDto>> getActiveTaxes();
    Mono<Void> deleteTaxe(UUID id);
}
