package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.PlanComptableTemplate;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Output port for PlanComptableTemplate persistence operations.
 */
public interface PlanComptableTemplateRepositoryPort {
    Mono<Boolean> existsByNumero(String numero);
}
