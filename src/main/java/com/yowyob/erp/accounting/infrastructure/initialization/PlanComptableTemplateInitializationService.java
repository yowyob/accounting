package com.yowyob.erp.accounting.infrastructure.initialization;

import com.yowyob.erp.accounting.domain.model.PlanComptableTemplate;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PlanComptableTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reactive Service to initialize the accounting plan template from a CSV file.
 */
@Service
@Slf4j
@org.springframework.core.annotation.Order(-1)
public class PlanComptableTemplateInitializationService implements CommandLineRunner {

    private final PlanComptableTemplateRepository template_repository;

    public PlanComptableTemplateInitializationService(PlanComptableTemplateRepository template_repository) {
        this.template_repository = template_repository;
    }

    @Override
    public void run(String... args) {
        log.info("Starting reactive accounting plan template initialization...");

        Mono.fromCallable(() -> {
            List<String[]> rows = new ArrayList<>();
            try (InputStream input_stream = getClass().getResourceAsStream("/plan_comptable_ohada_713.csv")) {
                if (input_stream == null) {
                    log.error("Reference CSV for accounting plan not found (plan_comptable_ohada_713.csv)!");
                    return rows;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input_stream))) {
                    String line;
                    boolean first_line = true;
                    while ((line = reader.readLine()) != null) {
                        if (first_line || line.isBlank()) {
                            first_line = false;
                            continue;
                        }
                        String[] data = line.split(";", -1);
                        if (data.length >= 3) {
                            rows.add(data);
                        }
                    }
                }
            }
            return rows;
        })
                .flatMapMany(Flux::fromIterable)
                .concatMap(data -> {
                    String numero = data[0].trim();
                    String libelle = data[1].trim();
                    // Garde-fou OHADA : un numéro de compte valide commence par un chiffre de
                    // classe 1..9 (jamais 0), et sa classe EST ce premier chiffre. On rejette les
                    // lignes corrompues (ex. anciens "011000 / Compte OHADA 11 / classe 6").
                    if (numero.isEmpty() || numero.charAt(0) < '1' || numero.charAt(0) > '9') {
                        log.warn("Skipping invalid OHADA account number (must start with class 1..9): {}", numero);
                        return Mono.empty();
                    }
                    int classe_from_numero = numero.charAt(0) - '0';
                    try {
                        int declared_classe = Integer.parseInt(data[2].trim());
                        if (declared_classe != classe_from_numero) {
                            log.warn("Skipping account {}: declared class {} != class from number {}",
                                    numero, declared_classe, classe_from_numero);
                            return Mono.empty();
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid class number for account {}: {}", numero, data[2]);
                        return Mono.empty();
                    }
                    // La classe persistée est toujours dérivée du numéro (source de vérité).
                    return createAccountIfNotExists(numero, libelle, classe_from_numero);
                })
                .then()
                .doOnSuccess(v -> log.info("Accounting plan template initialization completed."))
                .doOnError(e -> log.error("Error during accounting plan template initialization: {}", e.getMessage()))
                .block();
    }

    private Mono<Void> createAccountIfNotExists(String numero, String libelle, Integer classe) {
        return template_repository.existsByNumero(numero)
                .flatMap(exists -> {
                    if (!exists) {
                        log.info("Creating account template: {} - {}", numero, libelle);
                        PlanComptableTemplate plan = PlanComptableTemplate.builder()
                                .id(java.util.UUID.randomUUID())
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
                        return template_repository.save(plan).then();
                    }
                    return Mono.empty();
                });
    }
}
