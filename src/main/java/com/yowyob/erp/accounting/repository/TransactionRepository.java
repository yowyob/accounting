package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Transaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository interface for managing Transaction entities for R2DBC.
 */
@Repository
public interface TransactionRepository extends ReactiveCrudRepository<Transaction, UUID> {

       @Query("SELECT * FROM transactions WHERE organization_id = :organizationId AND id = :id")
       Mono<Transaction> findByTenantIdAndId(UUID organizationId, UUID id);

       @Query("SELECT * FROM transactions WHERE organization_id = :organizationId ORDER BY date_transaction DESC")
       Flux<Transaction> findByTenantIdOrderByDateTransactionDesc(UUID organizationId);

       @Query("SELECT * FROM transactions WHERE organization_id = :organizationId AND numero_recu = :numeroRecu")
       Mono<Transaction> findByTenantIdAndNumeroRecu(UUID organizationId, String numeroRecu);

       @Query("SELECT * FROM transactions WHERE organization_id = :organizationId AND est_comptabilisee = false")
       Flux<Transaction> findByTenantIdAndEstComptabiliseeFalse(UUID organizationId);

       @Query("SELECT * FROM transactions WHERE organization_id = :organizationId AND est_validee = false")
       Flux<Transaction> findByTenantIdAndEstValideeFalse(UUID organizationId);

       @Query("SELECT * FROM transactions WHERE organization_id = :organizationId AND date_transaction BETWEEN :startDate AND :endDate")
       Flux<Transaction> findByTenantIdAndDateRange(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate);

       @Query("SELECT COALESCE(SUM(montant_transaction), 0) FROM transactions WHERE organization_id = :organizationId AND est_validee = true")
       Mono<Double> getTotalValidatedTransactions(UUID organizationId);

       @Query("SELECT * FROM transactions WHERE organization_id = :organizationId AND caissier = :caissier")
       Flux<Transaction> findByTenantIdAndCaissier(UUID organizationId, String caissier);

       @Query("SELECT COUNT(*) > 0 FROM transactions WHERE organization_id = :organizationId AND numero_recu = :numeroRecu")
       Mono<Boolean> existsByTenantIdAndNumeroRecu(UUID organizationId, String numeroRecu);
}
