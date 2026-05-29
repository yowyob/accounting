package com.yowyob.erp.accounting.infrastructure.initialization;

import com.yowyob.erp.accounting.infrastructure.web.dto.CompteDto;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.CompteRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PlanComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.OrganizationRepository;
import com.yowyob.erp.accounting.domain.port.in.CompteUseCase;
import com.yowyob.erp.accounting.domain.port.in.PlanComptableUseCase;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;

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
 * Reactive Service to complete the setup for the default organization.
 */
@Service
@Order(6)
@Slf4j
@RequiredArgsConstructor
public class DefaultOrganizationSetupInitializationService implements CommandLineRunner {

    private final PlanComptableUseCase planComptableService;
    private final CompteUseCase compteService;
    private final PlanComptableRepository planRepository;
    private final CompteRepository compteRepository;
    private final OrganizationRepository organizationRepository;

    @Value("${app.organization.default-organization}")
    private String defaultOrganizationIdStr;

    @Override
    public void run(String... args) {
        UUID organizationId = UUID.fromString(defaultOrganizationIdStr);
        log.info("Starting default organization setup for ID: {}", organizationId);

        organizationRepository.findById(organizationId)
                .flatMap(organization -> {
                    log.info("Default organization found, proceeding with setup.");

                    // 1. Initialize Plan Comptable if empty
                    Mono<Void> planInit = planRepository.findByOrganization_Id(organizationId)
                            .collectList()
                            .flatMap(plans -> {
                                if (plans.isEmpty()) {
                                    log.info("Initializing Plan Comptable for default organization...");
                                    return planComptableService.initializePlanComptableForOrganization(organizationId)
                                            .then();
                                }
                                return Mono.empty();
                            });

                    // 2. Seed Essential Ledger Accounts
                    return planInit.then(seedEssentialAccounts(organizationId));
                })
                .doOnError(e -> log.error("Error during default organization setup: {}", e.getMessage()))
                .contextWrite(ReactiveOrganizationContext.withOrganizationId(organizationId))
                .subscribe(); // Subscribe to execute startup task
    }

    private Mono<Void> seedEssentialAccounts(UUID organizationId) {
        log.info("Seeding essential ledger accounts (Compte) for organization: {}", organizationId);

        return Flux.concat(
                createCompteIfNotExists(organizationId, "401000", "Fournisseurs", "PASSIF", 4),
                createCompteIfNotExists(organizationId, "411000", "Clients", "ACTIF", 4),
                createCompteIfNotExists(organizationId, "445710", "TVA Collectée", "PASSIF", 4),
                createCompteIfNotExists(organizationId, "445660", "TVA Déductible", "ACTIF", 4),
                createCompteIfNotExists(organizationId, "521000", "Banques", "ACTIF", 5),
                createCompteIfNotExists(organizationId, "571000", "Caisse", "ACTIF", 5),
                createCompteIfNotExists(organizationId, "601100", "Achats de marchandises", "CHARGE", 6),
                createCompteIfNotExists(organizationId, "701100", "Ventes de marchandises", "PRODUIT", 7)).then();
    }

    private Mono<Void> createCompteIfNotExists(UUID organizationId, String noCompte, String libelle, String type,
            Integer classe) {
        return compteRepository.existsByOrganization_IdAndNo_compte(organizationId, noCompte)
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
