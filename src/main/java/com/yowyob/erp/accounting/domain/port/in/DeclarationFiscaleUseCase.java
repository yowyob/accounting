package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.DeclarationFiscale;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.web.dto.DeclarationFiscaleDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the DeclarationFiscale operations.
 */
public interface DeclarationFiscaleUseCase {
    Mono<DeclarationFiscaleDto> saveDeclaration(DeclarationFiscaleDto dto);
    Mono<DeclarationFiscaleDto> getById(UUID id);
    Flux<DeclarationFiscaleDto> getAll();
    Flux<DeclarationFiscaleDto> getByType(String type);
    Flux<DeclarationFiscaleDto> getByPeriodRange(LocalDate start, LocalDate end);
    Mono<Void> delete(UUID id);
    Mono<DeclarationFiscaleDto> generateDeclaration(String type, LocalDate start, LocalDate end);
}
