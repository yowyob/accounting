package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Compte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for OHADA accounting accounts management.
 * Implements multi-tenant search operations and filters by account number and
 * class.
 * * @author ALD
 * 
 * @date 30.09.25
 */
@Repository
public interface CompteRepository extends JpaRepository<Compte, UUID> {

        /** Finds an account by tenant and account number */
        @Query("SELECT c FROM Compte c WHERE c.tenant.id = :tenant_id AND c.no_compte = :no_compte")
        Optional<Compte> findByTenant_IdAndNo_compte(@Param("tenant_id") UUID tenant_id,
                        @Param("no_compte") String no_compte);

        /** Finds an account by tenant and ID */
        @Query("SELECT c FROM Compte c WHERE c.tenant.id = :tenant_id AND c.id = :id")
        Optional<Compte> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

        /** Lists active accounts for a tenant */
        @Query("SELECT c FROM Compte c WHERE c.tenant.id = :tenant_id AND c.actif = true")
        List<Compte> findByTenant_IdAndActifTrue(@Param("tenant_id") UUID tenant_id);

        /** Lists accounts for a tenant by OHADA class */
        @Query("SELECT c FROM Compte c WHERE c.tenant.id = :tenant_id AND c.classe = :classe")
        List<Compte> findByTenant_IdAndClasse(@Param("tenant_id") UUID tenant_id, @Param("classe") Integer classe);

        /** Checks if an account exists for a tenant and a given number */
        @Query("SELECT COUNT(c) > 0 FROM Compte c WHERE c.tenant.id = :tenant_id AND c.no_compte = :no_compte")
        boolean existsByTenant_IdAndNo_compte(@Param("tenant_id") UUID tenant_id, @Param("no_compte") String no_compte);

        /** Finds accounts whose number starts with a given prefix */
        @Query("SELECT c FROM Compte c WHERE c.tenant.id = :tenant_id AND c.no_compte LIKE CONCAT(:prefix, '%')")
        List<Compte> findByTenant_IdAndNo_compteStartingWith(@Param("tenant_id") UUID tenant_id,
                        @Param("prefix") String prefix);

        /** Finds the latest account number for a given prefix (max value) */
        @Query("SELECT c FROM Compte c WHERE c.tenant.id = :tenant_id AND c.no_compte LIKE CONCAT(:prefix, '%') ORDER BY c.no_compte DESC LIMIT 1")
        Optional<Compte> findTopByTenant_IdAndNo_compteStartingWithOrderByNo_compteDesc(
                        @Param("tenant_id") UUID tenant_id,
                        @Param("prefix") String prefix);

        /** All accounts for a tenant (including inactive ones) */
        @Query("SELECT c FROM Compte c WHERE c.tenant.id = :tenant_id")
        List<Compte> findAllByTenant_Id(@Param("tenant_id") UUID tenant_id);
}
