package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Contrepartie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContrepartieRepository extends JpaRepository<Contrepartie, UUID> {

    List<Contrepartie> findByTenant_IdAndOperationComptable_Id(UUID tenantId, UUID operationComptableId);

    List<Contrepartie> findByTenant_IdAndCompte(String tenantId, String compte);

    @Transactional
    @Modifying
    @Query("DELETE FROM Contrepartie c WHERE c.tenant.id = :tenantId AND c.operationComptable.id = :operationComptableId")
    void deleteByTenantIdAndOperationComptableId(UUID tenantId, UUID operationComptableId);
}