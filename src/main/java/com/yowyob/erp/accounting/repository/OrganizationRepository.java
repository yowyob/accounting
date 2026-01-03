package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Organization entities.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Finds an organization by name.
     * 
     * @param name the organization name
     * @return an Optional containing the Organization if found
     */
    Optional<Organization> findByName(String name);
}
