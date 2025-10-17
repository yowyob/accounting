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

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

   Optional<Transaction> findByTenant_IdAndId(UUID tenantId, UUID id);
   
    List<Transaction> findByTenant_IdOrderByDateTransactionDesc(UUID tenantId);

    Optional<Transaction> findByTenant_IdAndNumeroRecu(UUID tenantId, String numeroRecu);

    List<Transaction> findByTenant_IdAndEstComptabiliseeFalse(UUID tenantId);

    List<Transaction> findByTenant_IdAndEstValideeFalse(UUID tenantId);

    @Query("""
           SELECT t FROM Transaction t 
           WHERE t.tenant.id = :tenantId 
           AND t.dateTransaction BETWEEN :startDate AND :endDate
           """)
    List<Transaction> findByTenant_IdAndDateRange(@Param("tenantId") UUID tenantId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    @Query("""
           SELECT COALESCE(SUM(t.montantTransaction), 0) 
           FROM Transaction t 
           WHERE t.tenant.id = :tenantId AND t.estValidee = true
           """)
    Double getTotalValidatedTransactions(@Param("tenantId") UUID tenantId);

    List<Transaction> findByTenant_IdAndCaissier(UUID tenantId, String caissier);

    boolean existsByTenantIdAndNumeroRecu(UUID tenantId, String numeroRecu);
}
