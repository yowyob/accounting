package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Transaction entities.
 * Updated to use snake_case field names to match the Java Entity.
 * * @author ALD
 * @date 30.12.25
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND t.id = :id")
    Optional<Transaction> findByTenant_IdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    @Query("SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId ORDER BY t.date_transaction DESC")
    List<Transaction> findByTenant_IdOrderByDateTransactionDesc(@Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND t.numero_recu = :numeroRecu")
    Optional<Transaction> findByTenant_IdAndNumeroRecu(@Param("tenantId") UUID tenantId, @Param("numeroRecu") String numeroRecu);

    @Query("SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND t.est_comptabilisee = false")
    List<Transaction> findByTenant_IdAndEstComptabiliseeFalse(@Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND t.est_validee = false")
    List<Transaction> findByTenant_IdAndEstValideeFalse(@Param("tenantId") UUID tenantId);

    @Query("""
           SELECT t FROM Transaction t 
           WHERE t.tenant.id = :tenantId 
           AND t.date_transaction BETWEEN :startDate AND :endDate
           """)
    List<Transaction> findByTenant_IdAndDateRange(@Param("tenantId") UUID tenantId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    @Query("""
           SELECT COALESCE(SUM(t.montant_transaction), 0) 
           FROM Transaction t 
           WHERE t.tenant.id = :tenantId AND t.est_validee = true
           """)
    Double getTotalValidatedTransactions(@Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND t.caissier = :caissier")
    List<Transaction> findByTenant_IdAndCaissier(@Param("tenantId") UUID tenantId, @Param("caissier") String caissier);

    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.tenant.id = :tenantId AND t.numero_recu = :numeroRecu")
    boolean existsByTenantIdAndNumeroRecu(@Param("tenantId") UUID tenantId, @Param("numeroRecu") String numeroRecu);
}
