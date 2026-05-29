package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.domain.model.TauxChange;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.TauxChangeDto;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the TauxChange operations.
 */
public interface TauxChangeUseCase {
    Mono<TauxChangeDto> createTauxChange(TauxChangeDto dto);
    Mono<List<TauxChangeDto>> getOrganizationRates();
    Mono<TauxChangeDto> getLatestRate(UUID sourceId, UUID targetId, LocalDateTime date);
    Mono<Void> deleteTauxChange(UUID id);
}
