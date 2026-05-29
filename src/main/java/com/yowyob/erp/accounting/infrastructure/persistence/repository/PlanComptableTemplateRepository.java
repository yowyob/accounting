package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.PlanComptableTemplateRepositoryPort;

import com.yowyob.erp.accounting.domain.model.PlanComptableTemplate;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for PlanComptableTemplate entity.
 */
@Repository
public interface PlanComptableTemplateRepository extends R2dbcRepository<PlanComptableTemplate, UUID>, PlanComptableTemplateRepositoryPort {

    Mono<Boolean> existsByNumero(String numero);
}
