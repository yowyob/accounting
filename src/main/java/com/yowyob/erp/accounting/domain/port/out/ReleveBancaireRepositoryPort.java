package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.ReleveBancaire;
import java.util.UUID;
import reactor.core.publisher.Flux;

/**
 * Output port for ReleveBancaire persistence operations.
 */
public interface ReleveBancaireRepositoryPort {
    Flux<ReleveBancaire> findByOrganizationIdAndCompteId(UUID organizationId, UUID compteId);
    Flux<ReleveBancaire> findByOrganizationIdAndCompteIdAndRapprocheFalse(UUID organizationId, UUID compteId);
}
