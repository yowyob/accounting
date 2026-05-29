package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.Budget;
import com.yowyob.erp.accounting.domain.model.BudgetLigneCompte;
import com.yowyob.erp.accounting.infrastructure.web.dto.BudgetDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.BudgetVsRealiseDto;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the Budget operations.
 */
public interface BudgetUseCase {
    Mono<BudgetDto> create(BudgetDto dto);
    Mono<BudgetDto> update(UUID id, BudgetDto dto);
    Mono<Void> delete(UUID id);
    Mono<BudgetDto> findById(UUID id);
    Flux<BudgetDto> getAll();
    Flux<BudgetDto> findByExercice(UUID exerciceId);
    Flux<BudgetDto> findByPeriode(UUID periodeId);
    Mono<BudgetDto> validate(UUID id);
    Mono<BudgetDto> activate(UUID id);
    Mono<BudgetDto> deactivate(UUID id);
    Mono<BudgetVsRealiseDto> getBudgetVsRealise(UUID exerciceId);
}
