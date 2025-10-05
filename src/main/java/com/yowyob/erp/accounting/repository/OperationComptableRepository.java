package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.OperationComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OperationComptableRepository extends JpaRepository<OperationComptable, Long> {

    List<OperationComptable> findByTenantId(UUID tenantId);

    Optional<OperationComptable> findByTenantIdAndId(UUID tenantId, Long id);

    Optional<OperationComptable> findByTenantIdAndTypeOperationAndModeReglement(UUID tenantId, String typeOperation, String modeReglement);

    List<OperationComptable> findByTenantIdAndComptePrincipal(UUID tenantId, String comptePrincipal);
}
