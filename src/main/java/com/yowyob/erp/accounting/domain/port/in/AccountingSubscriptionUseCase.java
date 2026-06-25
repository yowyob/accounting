package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.AccountingSubscription;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Use case port defining the AccountingSubscription operations (per organization).
 */
public interface AccountingSubscriptionUseCase {

    /**
     * Returns the organization's subscription, or a non-persisted default
     * (générale active, analytique inactive) when none exists yet.
     */
    Mono<AccountingSubscription> getForOrganization(UUID organizationId);

    /**
     * Creates or updates the organization's subscription to the accounting activities.
     */
    Mono<AccountingSubscription> updateForOrganization(UUID organizationId,
                                                       boolean generaleActive,
                                                       boolean analytiqueActive,
                                                       String user);
}
