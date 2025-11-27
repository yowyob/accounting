package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.repository.PlanComptableTemplateRepository;
import com.yowyob.erp.accounting.entity.PlanComptableTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Initialise le template de  plan comptable à partir d’un fichier CSV.
 * 
 * Fichier attendu : /resources/comptes_comptables.csv
 * Format :
 *    id,no_compte,libelle,classe
 */
@Service
public class PlanComptableTemplateInitializationService implements CommandLineRunner {

    private final PlanComptableTemplateRepository planComptableTemplateRepository;
    private final UUID tenantId;

    public PlanComptableTemplateInitializationService(
            PlanComptableTemplateRepository planComptableTemplateRepository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}")
            String tenantIdStr) {
        this.planComptableTemplateRepository = planComptableTemplateRepository;
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
                    String numero = data[0].trim();
                    String libelle = data[1].trim();
                    Integer classe = Integer.parseInt(data[2].trim());
                    createAccountIfNotExists(numero, libelle, classe);
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du plan comptable : " + e.getMessage());
        }
    }

    private void createAccountIfNotExists(String numero, String libelle, Integer classe) {
        boolean exists = planComptableTemplateRepository.existsByNumero( numero);
        if (!exists) {
            PlanComptableTemplate plan = PlanComptableTemplate.builder()
                    .numero(numero)
                    .libelle(libelle)
                    .classe(classe)
                    .notes("Compte initialisé automatiquement")
                    .actif(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy("system")
                    .updatedBy("system")
                    .build();
            planComptableTemplateRepository.save(plan);
        }
    }
}
