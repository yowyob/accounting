package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Immobilisation;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface ImmobilisationRepository extends R2dbcRepository<Immobilisation, UUID> {
    Flux<Immobilisation> findByTenantId(UUID organizationId);

    Flux<Immobilisation> findByTenantIdAndStatut(UUID organizationId, String statut);
}
