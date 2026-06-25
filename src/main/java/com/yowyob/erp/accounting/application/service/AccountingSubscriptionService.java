package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.AccountingSubscription;
import com.yowyob.erp.accounting.domain.port.in.AccountingSubscriptionUseCase;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.AccountingSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingSubscriptionService implements AccountingSubscriptionUseCase {

    private final AccountingSubscriptionRepository repository;

    @Override
    public Mono<AccountingSubscription> getForOrganization(UUID organizationId) {
        return repository.findByOrganizationId(organizationId)
                .switchIfEmpty(Mono.fromSupplier(() -> AccountingSubscription.builder()
                        .organizationId(organizationId)
                        .generaleActive(true)
                        .analytiqueActive(false)
                        .build()));
    }

    @Override
    public Mono<AccountingSubscription> updateForOrganization(UUID organizationId,
                                                              boolean generaleActive,
                                                              boolean analytiqueActive,
                                                              String user) {
        return repository.findByOrganizationId(organizationId)
                .switchIfEmpty(Mono.defer(() -> Mono.just(AccountingSubscription.builder()
                        .id(UUID.randomUUID())
                        .organizationId(organizationId)
                        .createdAt(LocalDateTime.now())
                        .createdBy(user)
                        .build())))
                .flatMap(subscription -> {
                    subscription.setGeneraleActive(generaleActive);
                    subscription.setAnalytiqueActive(analytiqueActive);
                    subscription.setUpdatedAt(LocalDateTime.now());
                    subscription.setUpdatedBy(user);
                    return repository.save(subscription);
                })
                .doOnSuccess(s -> log.info("Abonnement comptable mis à jour pour org={} : generale={}, analytique={}",
                        organizationId, generaleActive, analytiqueActive));
    }
}
