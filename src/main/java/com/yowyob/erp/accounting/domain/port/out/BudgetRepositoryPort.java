package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.Budget;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for Budget persistence operations.
 */
public interface BudgetRepositoryPort {
    Flux<Budget> findByOrganizationId(UUID organizationId);
    Flux<Budget> findByOrganizationIdAndExerciceId(UUID organizationId, UUID exerciceId);
    Flux<Budget> findByOrganizationIdAndPeriodeId(UUID organizationId, UUID periodeId);
    Flux<Budget> findByOrganizationIdAndParentId(UUID organizationId, UUID parentId);
    Flux<Budget> findByOrganizationIdAndType(UUID organizationId, String type);
}
