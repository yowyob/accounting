package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.PeriodeAnalytique;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface PeriodeAnalytiqueRepository extends R2dbcRepository<PeriodeAnalytique, UUID> {
    Flux<PeriodeAnalytique> findByOrganizationId(UUID organizationId);
    Flux<PeriodeAnalytique> findByOrganizationIdAndStatut(UUID organizationId, String statut);
    Flux<PeriodeAnalytique> findByOrganizationIdAndExerciceId(UUID organizationId, UUID exerciceId);
}
