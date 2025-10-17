package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository interface for managing Tenant entities.
 * Provides basic CRUD operations via JpaRepository.
 *
 * @author ALD
 * @date 12/10/2025 06:18 AM WAT
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}