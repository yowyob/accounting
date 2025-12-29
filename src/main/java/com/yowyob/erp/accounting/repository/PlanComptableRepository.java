package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PlanComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PlanComptable entity.
 * Provides access to the accounting plan accounts for each tenant.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface PlanComptableRepository extends JpaRepository<PlanComptable, UUID> {

    /** Finds an account by tenant and ID */
    Optional<PlanComptable> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

    /** Lists all accounts for a tenant */
    List<PlanComptable> findByTenant_Id(@Param("tenant_id") UUID tenant_id);

    /** Checks if an account exists by number for a tenant */
    @Query("SELECT COUNT(p) > 0 FROM PlanComptable p WHERE p.tenant.id = :tenant_id AND p.no_compte = :no_compte")
    boolean existsByTenant_IdAndNo_compte(@Param("tenant_id") UUID tenant_id, @Param("no_compte") String no_compte);

    /** Finds an account by number for a tenant */
    @Query("SELECT p FROM PlanComptable p WHERE p.tenant.id = :tenant_id AND p.no_compte = :no_compte")
    Optional<PlanComptable> findByTenant_IdAndNo_compte(@Param("tenant_id") UUID tenant_id,
            @Param("no_compte") String no_compte);

    /** Lists active accounts for a tenant */
    List<PlanComptable> findByTenant_IdAndActifTrue(@Param("tenant_id") UUID tenant_id);

    /** Lists accounts whose number starts with a prefix */
    @Query("SELECT p FROM PlanComptable p WHERE p.tenant.id = :tenant_id AND p.no_compte LIKE CONCAT(:prefix, '%')")
    List<PlanComptable> findByTenant_IdAndNo_compteStartingWith(@Param("tenant_id") UUID tenant_id,
            @Param("prefix") String prefix);

    /** Lists accounts by OHADA class */
    List<PlanComptable> findByTenant_IdAndClasse(@Param("tenant_id") UUID tenant_id, @Param("classe") Integer classe);
}
