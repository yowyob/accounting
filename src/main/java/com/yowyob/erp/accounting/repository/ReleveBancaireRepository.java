package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.ReleveBancaire;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface ReleveBancaireRepository extends R2dbcRepository<ReleveBancaire, UUID> {
    Flux<ReleveBancaire> findByOrganizationIdAndCompteId(UUID organizationId, UUID compteId);

    Flux<ReleveBancaire> findByOrganizationIdAndCompteIdAndRapprocheFalse(UUID organizationId, UUID compteId);
}
