package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.repository.PlanComptableTemplateRepository;
import com.yowyob.erp.accounting.entity.PlanComptableTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to initialize the accounting plan template from a CSV file.
 * Expected file: /resources/comptes_comptables.csv
 * Format:
 * numero|libelle|classe|sens|lettrable|collectif|niveau|compte_mere|actif
 * Follows snake_case naming and English Javadoc as per development charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@Slf4j
@org.springframework.core.annotation.Order(-1)
public class PlanComptableTemplateInitializationService implements CommandLineRunner {

    private final PlanComptableTemplateRepository template_repository;

    /**
     * Constructor for PlanComptableTemplateInitializationService.
     * 
     * @param template_repository the template repository
     */
    public PlanComptableTemplateInitializationService(PlanComptableTemplateRepository template_repository) {
        this.template_repository = template_repository;
    }

    @Override
    public void run(String... args) {
        logInitialization();
        try (InputStream input_stream = getClass().getResourceAsStream("/comptes_comptables.csv")) {

            if (input_stream == null) {
                log.error("Reference CSV for accounting plan not found!");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input_stream))) {
                String line;
                boolean first_line = true;

                while ((line = reader.readLine()) != null) {
                    if (first_line || line.isBlank()) {
                        first_line = false; // skip header or empty lines
                        continue;
                    }

                    // CSV uses '|' as separator
                    String[] data = line.split("\\|");
                    if (data.length >= 3) {
                        String numero = data[0].trim();
                        String libelle = data[1].trim();
                        try {
                            Integer classe = Integer.parseInt(data[2].trim());
                            createAccountIfNotExists(numero, libelle, classe);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid class number for account {}: {}", numero, data[2]);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error during accounting plan template initialization: {}", e.getMessage());
        }
    }

    private void logInitialization() {
        log.info("Starting accounting plan template initialization...");
    }

    /**
     * Creates an account template if it does not already exist.
     * 
     * @param numero  account number
     * @param libelle account label
     * @param classe  account class
     */
    private void createAccountIfNotExists(String numero, String libelle, Integer classe) {
        boolean exists = template_repository.existsByNumero(numero);
        if (!exists) {
            PlanComptableTemplate plan = PlanComptableTemplate.builder()
                    .numero(numero)
                    .libelle(libelle)
                    .classe(classe)
                    .notes("Automatically initialized account")
                    .actif(true)
                    .created_at(LocalDateTime.now())
                    .updated_at(LocalDateTime.now())
                    .created_by("system")
                    .updated_by("system")
                    .build();
            template_repository.save(plan);
        }
    }
}
