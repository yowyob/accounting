package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.OrganizationRepositoryPort;

import com.yowyob.erp.accounting.domain.model.Organization;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for Organization entity.
 */
@Repository
public interface OrganizationRepository extends R2dbcRepository<Organization, UUID>, OrganizationRepositoryPort {

    Mono<Organization> findByName(String name);
}
