package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.BrouillardComptableRepositoryPort;

import com.yowyob.erp.accounting.domain.model.BrouillardComptable;
import com.yowyob.erp.accounting.domain.model.BrouillardStatut;
import com.yowyob.erp.accounting.domain.model.BrouillardType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface BrouillardComptableRepository extends R2dbcRepository<BrouillardComptable, UUID>, BrouillardComptableRepositoryPort {

    Flux<BrouillardComptable> findAllByOrganizationIdAndStatut(UUID organizationId, BrouillardStatut statut, Pageable pageable);

    Flux<BrouillardComptable> findAllByOrganizationIdAndType(UUID organizationId, BrouillardType type, Pageable pageable);

    Mono<BrouillardComptable> findByOrganizationIdAndSourceIdAndSourceType(UUID organizationId, String sourceId, String sourceType);

    Mono<Long> countByOrganizationIdAndStatut(UUID organizationId, BrouillardStatut statut);

    Flux<BrouillardComptable> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Mono<Long> countByOrganizationId(UUID organizationId);
}
