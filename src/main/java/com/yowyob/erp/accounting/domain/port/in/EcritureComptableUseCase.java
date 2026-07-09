package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.DetailEcriture;
import com.yowyob.erp.accounting.domain.model.EcritureComptable;
import com.yowyob.erp.accounting.domain.model.EcritureStatut;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.application.service.CreateEcritureComptableResult;
import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import com.yowyob.erp.shared.domain.model.ComptableObject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the EcritureComptable operations.
 */
public interface EcritureComptableUseCase {
    Mono<CreateEcritureComptableResult> createEcriture(EcritureComptableDto dto, String idempotencyKey);

    default Mono<EcritureComptableDto> createEcriture(EcritureComptableDto dto) {
        return createEcriture(dto, null).map(CreateEcritureComptableResult::getDto);
    }
    Mono<EcritureComptableDto> updateEcriture(UUID id, EcritureComptableDto dto);
    Mono<EcritureComptableDto> validateEcriture(UUID id, String user);
    Mono<java.util.List<EcritureComptableDto>> getAll();
    Mono<java.util.List<EcritureComptableDto>> getNonValidated();
    Mono<EcritureComptableDto> getById(UUID id);
    Mono<java.util.List<EcritureComptableDto>> searchEcritures(LocalDateTime start_date, LocalDateTime end_date, UUID journal_id);
    Mono<java.util.List<EcritureComptableDto>> getByExercice(UUID exercice_id);
    Mono<EcritureComptableDto> generateFromComptableObject(ComptableObject object);
    Mono<Void> deleteEcriture(UUID id);
    Mono<EcritureComptableDto> cancelEcriture(UUID id, String user);
    Mono<Void> deactivateEcriture(UUID id);
}
