package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.MethodeCalculCout;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface MethodeCalculCoutRepository extends R2dbcRepository<MethodeCalculCout, UUID> {
    Flux<MethodeCalculCout> findByOrganizationId(UUID organizationId);
    Flux<MethodeCalculCout> findByOrganizationIdAndPlanAnalytiqueId(UUID organizationId, String planAnalytiqueId);
    Flux<MethodeCalculCout> findByOrganizationIdAndStatut(UUID organizationId, String statut);
}
