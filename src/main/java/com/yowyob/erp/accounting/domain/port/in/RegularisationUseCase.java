package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.RegularisationComptable;
import com.yowyob.erp.accounting.domain.model.StatutRegularisation;
import com.yowyob.erp.accounting.domain.model.TypeRegularisation;
import com.yowyob.erp.accounting.infrastructure.web.dto.RegularisationDto;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the Regularisation operations.
 */
public interface RegularisationUseCase {
    Mono<RegularisationDto> createRegularisation(RegularisationDto dto);
    Flux<RegularisationDto> getAll();
    Mono<RegularisationDto> getById(UUID id);
    Flux<RegularisationDto> getByPeriode(UUID periodeId);
    Flux<RegularisationDto> getByType(TypeRegularisation type);
    Flux<RegularisationDto> getByStatut(StatutRegularisation statut);
    Mono<RegularisationDto> extourner(UUID id);
    Mono<Long> extournerDues(UUID orgId, String user);
    Mono<Void> annuler(UUID id);
    void jobExtourneAutomatique();
}
