package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.ChargeVentilee;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface ChargeVentileeRepository extends R2dbcRepository<ChargeVentilee, UUID> {
    Flux<ChargeVentilee> findByOrganizationId(UUID organizationId);
    Flux<ChargeVentilee> findByOrganizationIdAndPeriodeId(UUID organizationId, UUID periodeId);
    Flux<ChargeVentilee> findByOrganizationIdAndIncorporable(UUID organizationId, Boolean incorporable);
    Flux<ChargeVentilee> findByOrganizationIdAndPeriodeIdAndIncorporable(
        UUID organizationId, UUID periodeId, Boolean incorporable);
}
