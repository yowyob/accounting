package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Organization;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for Organization entity.
 */
@Repository
public interface OrganizationRepository extends R2dbcRepository<Organization, UUID> {

    Mono<Organization> findByName(String name);
}
