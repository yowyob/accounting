package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.Organization;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Output port for Organization persistence operations.
 */
public interface OrganizationRepositoryPort {
    Mono<Organization> findByName(String name);
}
