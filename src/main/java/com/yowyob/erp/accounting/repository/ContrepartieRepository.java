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
public interface ContrepartieRepository extends JpaRepository<Contrepartie, Long> {

    List<Contrepartie> findByTenantIdAndOperationComptableId(UUID tenantId, Long operationComptableId);

    List<Contrepartie> findByTenantIdAndCompte(UUID tenantId, String compte);

    @Transactional
    @Modifying
    @Query("DELETE FROM Contrepartie c WHERE c.tenantId = :tenantId AND c.operationComptableId = :operationComptableId")
    void deleteByTenantIdAndOperationComptableId(UUID tenantId, Long operationComptableId);
}
