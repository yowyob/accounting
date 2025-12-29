package com.yowyob.erp.accounting.serviceInitialization;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
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
public class PeriodeComptableInitializationService implements CommandLineRunner {

    private final PeriodeComptableService periode_service;

    public PeriodeComptableInitializationService(PeriodeComptableService periode_service,
            @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}") String tenant_id_str) {
        this.periode_service = periode_service;
        TenantContext.setCurrentTenant(UUID.fromString(tenant_id_str));
    }

    @Override
    public void run(String... args) {
        for (int month = 1; month <= 12; month++) {
            String code = String.format("2025-%02d", month);
            LocalDate start_date = LocalDate.of(2025, month, 1);
            LocalDate end_date = start_date.withDayOfMonth(start_date.lengthOfMonth());

            PeriodeComptableDto dto = PeriodeComptableDto.builder()
                    .code(code)
                    .date_debut(start_date)
                    .date_fin(end_date)
                    .cloturee(false)
                    .build();
            try {
                periode_service.createPeriode(dto);
            } catch (IllegalArgumentException e) {
                // Skip if period already exists
            }
        }
    }
}