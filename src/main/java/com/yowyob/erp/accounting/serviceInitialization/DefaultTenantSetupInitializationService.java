package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.dto.CompteDto;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import com.yowyob.erp.accounting.repository.TenantRepository;
import com.yowyob.erp.accounting.service.CompteService;
import com.yowyob.erp.accounting.service.PlanComptableService;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service to complete the setup for the default tenant.
 * - Initializes the accounting plan (PlanComptable)
 * - Seeds essential ledger accounts (Compte)
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
    @Transactional
    public void run(String... args) {
        UUID tenantId = UUID.fromString(defaultTenantIdStr);
        log.info("Starting default tenant setup for ID: {}", tenantId);

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            log.warn("Default tenant not found, skipping setup.");
            return;
        }

        // Set context for services
        TenantContext.setCurrentTenant(tenantId);

        try {
            // 1. Initialize Plan Comptable if empty
            if (planRepository.findByTenant_Id(tenantId).isEmpty()) {
                log.info("Initializing Plan Comptable for default tenant...");
                planComptableService.initializePlanComptableForTenant(tenantId);
            }

            // 2. Seed Essential Ledger Accounts (Compte)
            seedEssentialAccounts(tenantId);

        } finally {
            TenantContext.clear();
        }
    }

    private void seedEssentialAccounts(UUID tenantId) {
        log.info("Seeding essential ledger accounts (Compte) for tenant: {}", tenantId);

        // Class 4 - Third parties
        createCompteIfNotExists(tenantId, "401000", "Fournisseurs", "PASSIF", 4);
        createCompteIfNotExists(tenantId, "411000", "Clients", "ACTIF", 4);
        createCompteIfNotExists(tenantId, "445710", "TVA Collectée", "PASSIF", 4);
        createCompteIfNotExists(tenantId, "445660", "TVA Déductible", "ACTIF", 4);

        // Class 5 - Treasury
        createCompteIfNotExists(tenantId, "521000", "Banques", "ACTIF", 5);
        createCompteIfNotExists(tenantId, "571000", "Caisse", "ACTIF", 5);

        // Class 6 - Charges
        createCompteIfNotExists(tenantId, "601100", "Achats de marchandises", "CHARGE", 6);

        // Class 7 - Products
        createCompteIfNotExists(tenantId, "701100", "Ventes de marchandises", "PRODUIT", 7);
    }

    private void createCompteIfNotExists(UUID tenantId, String noCompte, String libelle, String type, Integer classe) {
        if (!compteRepository.existsByTenant_IdAndNo_compte(tenantId, noCompte)) {
            log.info("Creating account: {} - {}", noCompte, libelle);
            CompteDto dto = CompteDto.builder()
                    .no_compte(noCompte)
                    .libelle(libelle)
                    .type_compte(type)
                    .classe(classe)
                    .solde(BigDecimal.ZERO)
                    .actif(true)
                    .build();
            compteService.createCompte(dto);
        }
    }
}
