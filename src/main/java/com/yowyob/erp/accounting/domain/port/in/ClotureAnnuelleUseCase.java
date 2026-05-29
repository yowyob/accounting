package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.ExerciceComptable;
import com.yowyob.erp.accounting.domain.model.PeriodeComptable;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the ClotureAnnuelle operations.
 */
public interface ClotureAnnuelleUseCase {
    Mono<Void> executerCloture(UUID exerciceId);
}
