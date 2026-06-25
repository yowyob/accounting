package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.AccountingSubscription;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Output port for AccountingSubscription persistence operations.
 */
public interface AccountingSubscriptionRepositoryPort {
    Mono<AccountingSubscription> findByOrganizationId(UUID organizationId);
}
