package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.AxeAnalytique;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for AxeAnalytique persistence operations.
 */
public interface AxeAnalytiqueRepositoryPort {
    Flux<AxeAnalytique> findByOrganizationId(UUID organizationId);
    Flux<AxeAnalytique> findByOrganizationIdAndActif(UUID organizationId, Boolean actif);
    Mono<AxeAnalytique> findByOrganizationIdAndCode(UUID organizationId, String code);
}
