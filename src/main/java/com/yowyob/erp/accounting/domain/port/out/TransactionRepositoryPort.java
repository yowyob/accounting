package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.Transaction;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for Transaction persistence operations.
 */
public interface TransactionRepositoryPort {
    Mono<Transaction> findByOrganizationIdAndId(UUID organizationId, UUID id);
    Flux<Transaction> findByOrganizationIdOrderByDateTransactionDesc(UUID organizationId);
    Mono<Transaction> findByOrganizationIdAndNumeroRecu(UUID organizationId, String numeroRecu);
    Flux<Transaction> findByOrganizationIdAndEstComptabiliseeFalse(UUID organizationId);
    Flux<Transaction> findByOrganizationIdAndEstValideeFalse(UUID organizationId);
    Flux<Transaction> findByOrganizationIdAndDateRange(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate);
    Mono<Double> getTotalValidatedTransactions(UUID organizationId);
    Flux<Transaction> findByOrganizationIdAndCaissier(UUID organizationId, String caissier);
    Mono<Boolean> existsByOrganizationIdAndNumeroRecu(UUID organizationId, String numeroRecu);
}
