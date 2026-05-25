package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.BudgetLigneCompte;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface BudgetLigneCompteRepository extends R2dbcRepository<BudgetLigneCompte, UUID> {
    Flux<BudgetLigneCompte> findByBudgetId(UUID budgetId);
    Mono<Void> deleteByBudgetId(UUID budgetId);
}
