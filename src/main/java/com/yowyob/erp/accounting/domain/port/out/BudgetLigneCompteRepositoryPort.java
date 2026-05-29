package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.BudgetLigneCompte;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for BudgetLigneCompte persistence operations.
 */
public interface BudgetLigneCompteRepositoryPort {
    Flux<BudgetLigneCompte> findByBudgetId(UUID budgetId);
    Mono<Void> deleteByBudgetId(UUID budgetId);
}
