package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Agence;
import com.yowyob.erp.accounting.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Agence entities.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Repository
public interface AgenceRepository extends JpaRepository<Agence, UUID> {

    /**
     * Finds all agencies for a given tenant.
     * 
     * @param tenant the tenant entity
     * @return a list of Agences
     */
    List<Agence> findByTenant(Tenant tenant);

    /**
     * Finds an agency by its code.
     * 
     * @param code the agency code
     * @return an Optional containing the Agence if found
     */
    Optional<Agence> findByCode(String code);

    /**
     * Finds an agency by tenant and code.
     * 
     * @param tenant the tenant entity
     * @param code   the agency code
     * @return an Optional containing the Agence if found
     */
    Optional<Agence> findByTenantAndCode(Tenant tenant, String code);
}
