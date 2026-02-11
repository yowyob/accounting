package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.BrouillardComptable;
import com.yowyob.erp.accounting.entity.BrouillardStatut;
import com.yowyob.erp.accounting.entity.BrouillardType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface BrouillardComptableRepository extends R2dbcRepository<BrouillardComptable, UUID> {

    Flux<BrouillardComptable> findAllByTenantIdAndStatut(UUID tenantId, BrouillardStatut statut, Pageable pageable);

    Flux<BrouillardComptable> findAllByTenantIdAndType(UUID tenantId, BrouillardType type, Pageable pageable);

    Mono<BrouillardComptable> findByTenantIdAndSourceIdAndSourceType(UUID tenantId, String sourceId, String sourceType);

    Mono<Long> countByTenantIdAndStatut(UUID tenantId, BrouillardStatut statut);

    Flux<BrouillardComptable> findAllByTenantId(UUID tenantId, Pageable pageable);

    Mono<Long> countByTenantId(UUID tenantId);
}
