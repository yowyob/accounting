package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.domain.model.PlanComptable;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.PlanComptableDto;
import com.yowyob.erp.shared.application.service.ValidationService;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the PlanComptable operations.
 */
public interface PlanComptableUseCase {
    Mono<Void> initializePlanComptableForOrganization(UUID organization_id);
    Mono<PlanComptableDto> createAccount(PlanComptableDto dto);
    Mono<List<PlanComptableDto>> getAllAccounts();
    Mono<List<PlanComptableDto>> getAllActiveAccounts();
    Mono<PlanComptableDto> getAccountById(UUID id);
    Mono<List<PlanComptableDto>> getAccountsByClass(Integer classe);
    Mono<List<PlanComptableDto>> getAccountsByPrefix(String prefix);
    Mono<PlanComptableDto> updateAccount(UUID id, PlanComptableDto dto);
    Mono<Void> deactivateAccount(UUID id);
}
