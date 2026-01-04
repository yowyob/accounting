package com.yowyob.erp.accounting.serviceInitialization;

import com.yowyob.erp.accounting.entity.ExerciceComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.ExerciceComptableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service to initialize default Fiscal Years (Exercice Comptable).
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Service
@Order(1)
public class ExerciceComptableInitializationService implements CommandLineRunner {

    private final ExerciceComptableRepository exercice_repository;
    private final UUID tenant_id;

    public ExerciceComptableInitializationService(
            ExerciceComptableRepository exercice_repository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
        this.exercice_repository = exercice_repository;
        this.tenant_id = UUID.fromString(tenant_id_str);
    }

    @Override
    @Transactional
    public void run(String... args) {
        createExerciceIfNotExists("2026", "Exercice 2026",
                LocalDate.of(2026, 1, 4), LocalDate.of(2026, 12, 31));
    }

    private void createExerciceIfNotExists(String code, String libelle, LocalDate start, LocalDate end) {
        if (exercice_repository.findByTenantAndCode(new Tenant(tenant_id), code).isEmpty()) {
            ExerciceComptable exercice = ExerciceComptable.builder()
                    .tenant(new Tenant(tenant_id))
                    .code(code)
                    .libelle(libelle)
                    .date_debut(start)
                    .date_fin(end)
                    .cloture(false)
                    .build();
            exercice_repository.save(exercice);
        }
    }
}
