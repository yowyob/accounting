package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.dto.CompteDto;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import com.yowyob.erp.accounting.repository.TenantRepository;
import com.yowyob.erp.accounting.service.CompteService;
import com.yowyob.erp.accounting.service.PlanComptableService;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reactive Service to complete the setup for the default tenant.
 */
@Service
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class DefaultTenantSetupInitializationService implements CommandLineRunner {

    private final PlanComptableService planComptableService;
    private final CompteService compteService;
    private final PlanComptableRepository planRepository;
    private final CompteRepository compteRepository;
    private final TenantRepository tenantRepository;

    @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}")
    private String defaultTenantIdStr;

    @Override
    public void run(String... args) {
        UUID tenantId = UUID.fromString(defaultTenantIdStr);
        log.info("Starting default tenant setup for ID: {}", tenantId);

        tenantRepository.findById(tenantId)
                .flatMap(tenant -> {
                    log.info("Default tenant found, proceeding with setup.");

                    // 1. Initialize Plan Comptable if empty
                    Mono<Void> planInit = planRepository.findByTenant_Id(tenantId)
                            .collectList()
                            .flatMap(plans -> {
                                if (plans.isEmpty()) {
                                    log.info("Initializing Plan Comptable for default tenant...");
                                    return planComptableService.initializePlanComptableForTenant(tenantId).then();
                                }
                                return Mono.empty();
                            });

                    // 2. Seed Essential Ledger Accounts
                    return planInit.then(seedEssentialAccounts(tenantId));
                })
                .doOnError(e -> log.error("Error during default tenant setup: {}", e.getMessage()))
                .contextWrite(ReactiveTenantContext.withTenantId(tenantId))
                .subscribe(); // Subscribe to execute startup task
    }

    private Mono<Void> seedEssentialAccounts(UUID tenantId) {
        log.info("Seeding essential ledger accounts (Compte) for tenant: {}", tenantId);

        return Flux.concat(
                createCompteIfNotExists(tenantId, "401000", "Fournisseurs", "PASSIF", 4),
                createCompteIfNotExists(tenantId, "411000", "Clients", "ACTIF", 4),
                createCompteIfNotExists(tenantId, "445710", "TVA Collectée", "PASSIF", 4),
                createCompteIfNotExists(tenantId, "445660", "TVA Déductible", "ACTIF", 4),
                createCompteIfNotExists(tenantId, "521000", "Banques", "ACTIF", 5),
                createCompteIfNotExists(tenantId, "571000", "Caisse", "ACTIF", 5),
                createCompteIfNotExists(tenantId, "601100", "Achats de marchandises", "CHARGE", 6),
                createCompteIfNotExists(tenantId, "701100", "Ventes de marchandises", "PRODUIT", 7)).then();
    }

    private Mono<Void> createCompteIfNotExists(UUID tenantId, String noCompte, String libelle, String type,
            Integer classe) {
        return compteRepository.existsByTenant_IdAndNo_compte(tenantId, noCompte)
                .flatMap(exists -> {
                    if (!exists) {
                        log.info("Creating account: {} - {}", noCompte, libelle);
                        CompteDto dto = CompteDto.builder()
                                .no_compte(noCompte)
                                .libelle(libelle)
                                .type_compte(type)
                                .classe(classe)
                                .solde(BigDecimal.ZERO)
                                .actif(true)
                                .build();
                        return compteService.createCompte(dto).then();
                    }
                    return Mono.empty();
                });
    }
}
