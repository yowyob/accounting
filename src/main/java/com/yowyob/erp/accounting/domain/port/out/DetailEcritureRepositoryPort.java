package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.DetailEcriture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for DetailEcriture persistence operations.
 */
public interface DetailEcritureRepositoryPort {
    Flux<DetailEcriture> findByOrganizationId(UUID organization_id);
}
