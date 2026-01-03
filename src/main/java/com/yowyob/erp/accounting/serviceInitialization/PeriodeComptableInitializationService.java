package com.yowyob.erp.accounting.serviceInitialization;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
import com.yowyob.erp.accounting.entity.ExerciceComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.service.PeriodeComptableService;
import com.yowyob.erp.config.tenant.TenantContext;

/**
 * Service for initializing default accounting periods for the default tenant.
 * Runs on application startup.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@Order(2)
public class PeriodeComptableInitializationService implements CommandLineRunner {

    private final PeriodeComptableService periode_service;
    private final com.yowyob.erp.accounting.repository.ExerciceComptableRepository exercice_repository;
    private final UUID tenant_id;

    public PeriodeComptableInitializationService(PeriodeComptableService periode_service,
            com.yowyob.erp.accounting.repository.ExerciceComptableRepository exercice_repository,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
        this.periode_service = periode_service;
        this.exercice_repository = exercice_repository;
        this.tenant_id = UUID.fromString(tenant_id_str);
        TenantContext.setCurrentTenant(this.tenant_id);
    }

    @Override
    public void run(String... args) {
        ExerciceComptable exercice = exercice_repository
                .findByTenantAndCode(new Tenant(tenant_id), "2026")
                .orElseThrow(() -> new RuntimeException("L'exercice 2026 n'a pas été trouvé. Initialisation avortée."));

        UUID exercice_id = exercice.getId();
        // Run for 2026
        for (int month = 1; month <= 12; month++) {
            String code = String.format("2026-%02d", month);
            LocalDate start_date = LocalDate.of(2026, month, 1);
            LocalDate end_date = start_date.withDayOfMonth(start_date.lengthOfMonth());

            PeriodeComptableDto dto = PeriodeComptableDto.builder()
                    .exercice_id(exercice_id)
                    .code(code)
                    .date_debut(start_date)
                    .date_fin(end_date)
                    .cloturee(false)
                    .build();
            try {
                periode_service.createPeriode(dto);
            } catch (Exception e) {
                System.err.println("Erreur création période " + code + ": " + e.getMessage());
            }
        }
    }
}