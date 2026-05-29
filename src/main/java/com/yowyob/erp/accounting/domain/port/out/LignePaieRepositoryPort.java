package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.LignePaie;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for LignePaie persistence operations.
 */
public interface LignePaieRepositoryPort {
    Flux<LignePaie> findByOrganizationId(UUID organizationId);
    Flux<LignePaie> findByOrganizationIdAndExerciceId(UUID organizationId, UUID exerciceId);
}
