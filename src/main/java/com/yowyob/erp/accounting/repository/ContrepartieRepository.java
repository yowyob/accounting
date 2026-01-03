package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Contrepartie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContrepartieRepository extends JpaRepository<Contrepartie, UUID> {

    /**
     * Finds counterparties by tenant and operation.
     * 
     * @param tenant_id              the tenant ID
     * @param operation_comptable_id the operation ID
     * @return list of counterparties
     */
    @Query("SELECT c FROM Contrepartie c WHERE c.tenant.id = :tenant_id AND c.operation_comptable.id = :operation_comptable_id")
    List<Contrepartie> findByTenant_IdAndOperation_comptable_Id(@Param("tenant_id") UUID tenant_id,
            @Param("operation_comptable_id") UUID operation_comptable_id);

    /**
     * Finds counterparties by tenant and account number.
     * 
     * @param tenant_id the tenant ID
     * @param compte    the account number
     * @return list of counterparties
     */
    List<Contrepartie> findByTenant_IdAndCompte(UUID tenant_id, String compte);

    /**
     * Deletes counterparties for a specific operation.
     * 
     * @param tenant_id              the tenant ID
     * @param operation_comptable_id the operation ID
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Contrepartie c WHERE c.tenant.id = :tenant_id AND c.operation_comptable.id = :operation_comptable_id")
    void deleteByTenantIdAndOperationComptableId(@Param("tenant_id") UUID tenant_id,
            @Param("operation_comptable_id") UUID operation_comptable_id);
}