package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.AmortissementLigne;
import com.yowyob.erp.accounting.domain.model.ExerciceComptable;
import com.yowyob.erp.accounting.domain.model.Immobilisation;
import com.yowyob.erp.accounting.infrastructure.web.dto.AmortissementLigneDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.ImmobilisationDto;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the Immobilisation operations.
 */
public interface ImmobilisationUseCase {
    Mono<ImmobilisationDto> create(ImmobilisationDto dto);
    Flux<ImmobilisationDto> findAll();
    Flux<ImmobilisationDto> findByStatut(String statut);
    Mono<ImmobilisationDto> findById(UUID id);
    Mono<ImmobilisationDto> update(UUID id, ImmobilisationDto dto);
    Mono<Void> delete(UUID id);
    Mono<ImmobilisationDto> ceder(UUID id, ImmobilisationDto dto);
    Mono<ImmobilisationDto> mettreAuRebut(UUID id);
    Flux<AmortissementLigneDto> getTableauAmortissement(UUID id);
    Mono<Void> genererTableauAmortissement(UUID immoId);
    Mono<Void> genererTableauAmortissementUnitesProduction(UUID immoId, List<BigDecimal> unitesByYear);
    Mono<Void> comptabiliserAmortissements(UUID exerciceId);
}
