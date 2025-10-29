package com.yowyob.erp.accounting.serviceInitialization;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
import com.yowyob.erp.accounting.service.PeriodeComptableService;
import com.yowyob.erp.config.tenant.TenantContext;

@Service
public class PeriodeComptableInitializationService implements CommandLineRunner {

    private final PeriodeComptableService periodeComptableService;

    public PeriodeComptableInitializationService(PeriodeComptableService periodeComptableService,
    @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}")
     String tenantIdStr) {
        this.periodeComptableService = periodeComptableService;
        TenantContext.setCurrentTenant(UUID.fromString(tenantIdStr));
    }

    @Override
    public void run(String... args) {
        
        for (int month = 1; month <= 12; month++) {
            String code = String.format("2025-%02d", month);
            LocalDate startDate = LocalDate.of(2025, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            PeriodeComptableDto dto = PeriodeComptableDto.builder()
                    .code(code)
                    .dateDebut(startDate)
                    .dateFin(endDate)
                    .cloturee(false)
                    .build();
            try {
                periodeComptableService.createPeriode(dto);
            } catch (IllegalArgumentException e) {
                // Skip if period already exists
            }
        }
    }
}