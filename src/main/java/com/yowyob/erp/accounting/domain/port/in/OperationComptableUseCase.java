package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.Compte;
import com.yowyob.erp.accounting.domain.model.Contrepartie;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.JournalComptable;
import com.yowyob.erp.accounting.domain.model.OperationComptable;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.web.dto.ContrepartieDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.OperationComptableDto;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the OperationComptable operations.
 */
public interface OperationComptableUseCase {
    Mono<OperationComptableDto> createOperation(OperationComptableDto dto);
    Mono<List<OperationComptableDto>> getAllOperations();
    Mono<OperationComptableDto> getOperation(UUID id);
    Mono<List<OperationComptableDto>> getOperationsByCompteId(UUID compte_id);
    Mono<List<OperationComptableDto>> getOperationsByCompte(String no_compte);
    Mono<OperationComptableDto> getByTypeAndMode(String type, String mode);
    Mono<OperationComptableDto> updateOperation(UUID id, OperationComptableDto dto);
    Mono<Void> deleteOperation(UUID id);
}
