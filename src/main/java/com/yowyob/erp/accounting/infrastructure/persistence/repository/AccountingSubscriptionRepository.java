package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.AccountingSubscription;
import com.yowyob.erp.accounting.domain.port.out.AccountingSubscriptionRepositoryPort;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AccountingSubscriptionRepository
        extends R2dbcRepository<AccountingSubscription, UUID>, AccountingSubscriptionRepositoryPort {

    Mono<AccountingSubscription> findByOrganizationId(UUID organizationId);
}
