package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.OperationComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    Optional<OperationComptable> findByTenant_IdAndType_operationAndMode_reglement(UUID tenant_id,
            String type_operation, String mode_reglement);

    List<OperationComptable> findByTenant_IdAndCompte_principal(UUID tenant_id, String compte_principal);
}
