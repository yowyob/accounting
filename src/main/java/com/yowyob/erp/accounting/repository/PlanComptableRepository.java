package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PlanComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanComptableRepository extends JpaRepository<PlanComptable, Long> {

    List<PlanComptable> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndNoCompte(UUID tenantId, String noCompte);

    Optional<PlanComptable> findByTenantIdAndNoCompte(UUID tenantId, String noCompte);

    List<PlanComptable> findByTenantIdAndActifTrue(UUID tenantId);

    @Query("SELECT p FROM PlanComptable p WHERE p.tenantId = :tenantId AND p.noCompte LIKE CONCAT(:prefix, '%')")
    List<PlanComptable> findByTenantIdAndNoCompteStartingWith(UUID tenantId, String prefix);

    List<PlanComptable> findByTenantIdAndClasse(UUID tenantId, Integer classe);
}
