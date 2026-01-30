package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PlanComptableTemplate;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for PlanComptableTemplate entity.
 */
@Repository
public interface PlanComptableTemplateRepository extends R2dbcRepository<PlanComptableTemplate, UUID> {

    Mono<Boolean> existsByNumero(String numero);
}
