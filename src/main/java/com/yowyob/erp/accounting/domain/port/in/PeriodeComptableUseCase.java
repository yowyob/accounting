package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.domain.model.PeriodeComptable;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.PeriodeComptableDto;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the PeriodeComptable operations.
 */
public interface PeriodeComptableUseCase {
    Mono<PeriodeComptableDto> createPeriode(PeriodeComptableDto dto);
    Mono<PeriodeComptableDto> getPeriode(UUID id);
    Mono<List<PeriodeComptableDto>> getAllPeriodes();
    Mono<PeriodeComptableDto> getByCode(String code);
    Mono<PeriodeComptableDto> getByDate(LocalDate date);
    Mono<List<PeriodeComptableDto>> getNonClosedPeriodes();
    Mono<List<PeriodeComptableDto>> getByRange(LocalDate start, LocalDate end);
    Mono<PeriodeComptableDto> getCurrentPeriode(UUID organization_id);
    Mono<PeriodeComptableDto> updatePeriode(UUID id, PeriodeComptableDto dto);
    Mono<PeriodeComptableDto> closePeriode(UUID id);
    Mono<Void> deletePeriode(UUID id);
}
