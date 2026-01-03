package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.Organization;
import com.yowyob.erp.accounting.repository.OrganizationRepository;
import com.yowyob.erp.accounting.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to initialize default Organizations and link existing Tenants.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Slf4j
@Service
@Order(0) // Run before other initializers
@RequiredArgsConstructor
public class OrganizationInitializationService implements CommandLineRunner {

    private final OrganizationRepository organization_repository;
    private final TenantRepository tenant_repository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing default organizations...");

        Organization default_org = organization_repository.findByName("YOWYOB Group")
                .orElseGet(() -> {
                    log.info("Creating default organization: YOWYOB Group");
                    Organization org = Organization.builder()
                            .name("YOWYOB Group")
                            .description("Default Group Organization")
                            .address("Yaounde, Cameroon")
                            .tax_id("TAX-001")
                            .build();
                    return organization_repository.save(org);
                });

        // Link all orphans tenants to the default organization
        tenant_repository.findAll().stream()
                .filter(t -> t.getOrganization() == null)
                .forEach(t -> {
                    log.info("Linking tenant {} to organization {}", t.getCode(), default_org.getName());
                    t.setOrganization(default_org);
                    tenant_repository.save(t);
                });
    }
}
