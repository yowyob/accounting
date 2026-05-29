package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.Immobilisation;
import java.util.UUID;
import reactor.core.publisher.Flux;

/**
 * Output port for Immobilisation persistence operations.
 */
public interface ImmobilisationRepositoryPort {
    Flux<Immobilisation> findByOrganizationId(UUID organizationId);
    Flux<Immobilisation> findByOrganizationIdAndStatut(UUID organizationId, String statut);
}
