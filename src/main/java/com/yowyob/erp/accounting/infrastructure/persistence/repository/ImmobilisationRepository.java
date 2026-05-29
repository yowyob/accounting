package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.ImmobilisationRepositoryPort;

import com.yowyob.erp.accounting.domain.model.Immobilisation;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface ImmobilisationRepository extends R2dbcRepository<Immobilisation, UUID>, ImmobilisationRepositoryPort {
    Flux<Immobilisation> findByOrganizationId(UUID organizationId);

    Flux<Immobilisation> findByOrganizationIdAndStatut(UUID organizationId, String statut);
}
