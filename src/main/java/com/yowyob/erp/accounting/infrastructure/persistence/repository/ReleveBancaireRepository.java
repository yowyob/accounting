package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.ReleveBancaireRepositoryPort;

import com.yowyob.erp.accounting.domain.model.ReleveBancaire;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface ReleveBancaireRepository extends R2dbcRepository<ReleveBancaire, UUID>, ReleveBancaireRepositoryPort {
    Flux<ReleveBancaire> findByOrganizationIdAndCompteId(UUID organizationId, UUID compteId);

    Flux<ReleveBancaire> findByOrganizationIdAndCompteIdAndRapprocheFalse(UUID organizationId, UUID compteId);
}
