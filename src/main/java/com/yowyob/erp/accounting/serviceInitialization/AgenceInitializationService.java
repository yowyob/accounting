package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.Agence;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.AgenceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service to initialize default Agencies (Agences).
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Service
@Order(1)
public class AgenceInitializationService implements CommandLineRunner {

    private final AgenceRepository agence_repository;
    private final UUID tenant_id;

    public AgenceInitializationService(
            AgenceRepository agence_repository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
        this.agence_repository = agence_repository;
        this.tenant_id = UUID.fromString(tenant_id_str);
    }

    @Override
    @Transactional
    public void run(String... args) {
        createAgenceIfNotExists("HQ", "Headquarters", "Yaounde", "Cameroon");
    }

    private void createAgenceIfNotExists(String code, String name, String city, String country) {
        if (agence_repository.findByTenantAndCode(new Tenant(tenant_id), code).isEmpty()) {
            Agence agence = Agence.builder()
                    .tenant(new Tenant(tenant_id))
                    .code(code)
                    .name(name)
                    .city(city)
                    .country(country)
                    .address("Default Street")
                    .build();
            agence_repository.save(agence);
        }
    }
}
