package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.OperationComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for managing OperationComptable entities.
 * Supports retrieval by tenant, type, settlement mode, and principal account.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface OperationComptableRepository extends JpaRepository<OperationComptable, UUID> {

    List<OperationComptable> findByTenant_Id(UUID tenant_id);

    Optional<OperationComptable> findByTenant_IdAndId(UUID tenant_id, UUID id);

    @Query("SELECT o FROM OperationComptable o WHERE o.tenant.id = :tenantId AND o.compte_principal = :comptePrincipal")
    List<OperationComptable> findByTenant_IdAndCompte_principal(
            @Param("tenantId") UUID tenantId,
            @Param("comptePrincipal") String comptePrincipal);

    @Query("SELECT o FROM OperationComptable o WHERE o.tenant.id = :tenantId AND o.type_operation = :typeOp AND o.mode_reglement = :modeReg")
    Optional<OperationComptable> findByTenant_IdAndType_operationAndMode_reglement(
            @Param("tenantId") UUID tenantId,
            @Param("typeOp") String typeOp,
            @Param("modeReg") String modeReg);
}
