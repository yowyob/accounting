package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PlanComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanComptableRepository extends JpaRepository<PlanComptable, UUID> {

    Optional<PlanComptable> findByTenant_IdAndId(UUID tenantId , UUID id);
    List<PlanComptable> findByTenant_Id(UUID tenantId);

    boolean existsByTenantIdAndNoCompte(UUID tenantId, String noCompte);

    Optional<PlanComptable> findByTenant_IdAndNoCompte(UUID tenantId, String noCompte);

    List<PlanComptable> findByTenant_IdAndActifTrue(UUID tenantId);

    @Query("SELECT p FROM PlanComptable p WHERE p.tenant.id = :tenantId AND p.noCompte LIKE CONCAT(:prefix, '%')")
    List<PlanComptable> findByTenant_IdAndNoCompteStartingWith(UUID tenantId, String prefix);

    List<PlanComptable> findByTenant_IdAndClasse(UUID tenantId, Integer classe);
}
