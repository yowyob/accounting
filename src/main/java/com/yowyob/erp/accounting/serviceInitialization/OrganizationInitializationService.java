package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.Organization;
import com.yowyob.erp.accounting.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive Service to initialize default Organizations.
 */
@Slf4j
@Service
@Order(0)
@RequiredArgsConstructor
@DependsOn("liquibase")
public class OrganizationInitializationService implements CommandLineRunner {

    private final OrganizationRepository organization_repository;

    @Value("${app.organization.default-organization:4e177ff2-89b8-4d24-926a-5763dfa1b19a}")
    private String default_organization_id_str;

    @Override
    public void run(String... args) {
        log.info("Initializing default organizations (reactive)...");
        UUID organization_id = UUID.fromString(default_organization_id_str);

        organization_repository.findById(organization_id)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Default organization not found by ID, checking by name...");
                    return organization_repository.findByName("YOWYOB Group");
                }))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Creating default organization: YOWYOB Group");
                    Organization org = Organization.builder()
                            .id(organization_id)
                            .code("YOWYOB-GRP")
                            .name("YOWYOB Group")
                            .description("Default Group Organization")
                            .address("Yaounde, Cameroon")
                            .tax_id("TAX-001")
                            .isNew(true)
                            .build();
                    return organization_repository.save(org);
                }))
                .doOnSuccess(v -> log.info("Organization initialization completed."))
                .doOnError(e -> log.error("Error during organization initialization: {}", e.getMessage()))
                .block();
    }
}
