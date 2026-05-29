package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.AmortissementLigne;
import java.util.UUID;
import reactor.core.publisher.Flux;

/**
 * Output port for AmortissementLigne persistence operations.
 */
public interface AmortissementLigneRepositoryPort {
    Flux<AmortissementLigne> findByImmoId(UUID immoId);
    Flux<AmortissementLigne> findByExerciceId(UUID exerciceId);
    Flux<AmortissementLigne> findByExerciceIdAndComptabiliseeFalse(UUID exerciceId);
}
