package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.Organization;
import com.yowyob.erp.accounting.repository.OrganizationRepository;
import com.yowyob.erp.accounting.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Reactive Service to initialize default Organizations and link existing
 * Tenants.
 */
@Slf4j
@Service
@Order(0)
@RequiredArgsConstructor
public class OrganizationInitializationService implements CommandLineRunner {

    private final OrganizationRepository organization_repository;
    private final TenantRepository tenant_repository;

    @Override
    public void run(String... args) {
        log.info("Initializing default organizations (reactive)...");

        organization_repository.findByName("YOWYOB Group")
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Creating default organization: YOWYOB Group");
                    Organization org = Organization.builder()
                            .name("YOWYOB Group")
                            .description("Default Group Organization")
                            .address("Yaounde, Cameroon")
                            .tax_id("TAX-001")
                            .build();
                    return organization_repository.save(org);
                }))
                .flatMap(default_org -> {
                    // Link all orphans tenants to the default organization
                    return tenant_repository.findAll()
                            .filter(t -> t.getOrganization() == null)
                            .flatMap(t -> {
                                log.info("Linking tenant {} to organization {}", t.getCode(), default_org.getName());
                                t.setOrganization(default_org);
                                return tenant_repository.save(t);
                            })
                            .then();
                })
                .doOnSuccess(v -> log.info("Organization initialization completed."))
                .doOnError(e -> log.error("Error during organization initialization: {}", e.getMessage()))
                .subscribe();
    }
}
