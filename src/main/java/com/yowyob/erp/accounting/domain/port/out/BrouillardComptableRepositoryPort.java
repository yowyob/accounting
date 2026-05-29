package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.BrouillardComptable;
import com.yowyob.erp.accounting.domain.model.BrouillardStatut;
import com.yowyob.erp.accounting.domain.model.BrouillardType;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for BrouillardComptable persistence operations.
 */
public interface BrouillardComptableRepositoryPort {
    Flux<BrouillardComptable> findAllByOrganizationIdAndStatut(UUID organizationId, BrouillardStatut statut, Pageable pageable);
    Flux<BrouillardComptable> findAllByOrganizationIdAndType(UUID organizationId, BrouillardType type, Pageable pageable);
    Mono<BrouillardComptable> findByOrganizationIdAndSourceIdAndSourceType(UUID organizationId, String sourceId, String sourceType);
    Mono<Long> countByOrganizationIdAndStatut(UUID organizationId, BrouillardStatut statut);
    Flux<BrouillardComptable> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    Mono<Long> countByOrganizationId(UUID organizationId);
}
