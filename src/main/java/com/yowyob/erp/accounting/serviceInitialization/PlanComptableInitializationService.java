package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.PlanComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Initialise le plan comptable à partir d’un fichier CSV.
 * 
 * Fichier attendu : /resources/comptes_comptables.csv
 * Format :
 *    id,no_compte,libelle,classe
 */
@Service
public class PlanComptableInitializationService implements CommandLineRunner {

    private final PlanComptableRepository planComptableRepository;
    private final UUID tenantId;

    public PlanComptableInitializationService(
            PlanComptableRepository planComptableRepository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}")
            String tenantIdStr) {
        this.planComptableRepository = planComptableRepository;
        this.tenantId = UUID.fromString(tenantIdStr);
    }

    @Override
    public void run(String... args) {
        try (InputStream inputStream = getClass().getResourceAsStream("/comptes_comptables.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // skip header
                    continue;
                }

                String[] data = line.split(",");
                if (data.length >= 4) {
                    String noCompte = data[1].trim();
                    String libelle = data[2].trim();
                    Integer classe = Integer.parseInt(data[3].trim());
                    createAccountIfNotExists(noCompte, libelle, classe);
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du plan comptable : " + e.getMessage());
        }
    }

    private void createAccountIfNotExists(String noCompte, String libelle, Integer classe) {
        boolean exists = planComptableRepository.existsByTenantIdAndNoCompte(tenantId, noCompte);
        if (!exists) {
            PlanComptable plan = PlanComptable.builder()
                    .tenant(new Tenant(tenantId))
                    .noCompte(noCompte)
                    .libelle(libelle)
                    .classe(classe)
                    .notes("Compte initialisé automatiquement")
                    .actif(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy("system")
                    .updatedBy("system")
                    .build();
            planComptableRepository.save(plan);
        }
    }
}
