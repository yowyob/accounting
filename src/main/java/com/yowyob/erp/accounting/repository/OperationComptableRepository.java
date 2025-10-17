package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.OperationComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OperationComptableRepository extends JpaRepository<OperationComptable, UUID> {

    List<OperationComptable> findByTenant_Id(UUID tenantId);

    Optional<OperationComptable> findByTenant_IdAndId(UUID tenantId, UUID id);

    Optional<OperationComptable> findByTenant_IdAndTypeOperationAndModeReglement(UUID tenantId, String typeOperation, String modeReglement);

    List<OperationComptable> findByTenant_IdAndComptePrincipal(UUID tenantId, String comptePrincipal);
}
