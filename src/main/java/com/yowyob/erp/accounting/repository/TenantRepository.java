package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Tenant;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * R2DBC Repository interface for managing Tenant entities.
 */
@Repository
public interface TenantRepository extends R2dbcRepository<Tenant, UUID> {
}