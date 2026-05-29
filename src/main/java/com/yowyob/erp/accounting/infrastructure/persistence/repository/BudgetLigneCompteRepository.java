package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.BudgetLigneCompteRepositoryPort;

import com.yowyob.erp.accounting.domain.model.BudgetLigneCompte;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface BudgetLigneCompteRepository extends R2dbcRepository<BudgetLigneCompte, UUID>, BudgetLigneCompteRepositoryPort {
    Flux<BudgetLigneCompte> findByBudgetId(UUID budgetId);
    Mono<Void> deleteByBudgetId(UUID budgetId);
}
