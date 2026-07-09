package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.CompteAnalytique;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CompteAnalytiqueRepositoryPort {
    Flux<CompteAnalytique> findByOrganizationId(UUID organizationId);
    Flux<CompteAnalytique> findByOrganizationIdAndActif(UUID organizationId, Boolean actif);
    Mono<CompteAnalytique> findByOrganizationIdAndCode(UUID organizationId, String code);
}
