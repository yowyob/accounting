package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.AmortissementLigne;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface AmortissementLigneRepository extends R2dbcRepository<AmortissementLigne, UUID> {
    Flux<AmortissementLigne> findByImmoId(UUID immoId);

    Flux<AmortissementLigne> findByExerciceId(UUID exerciceId);

    Flux<AmortissementLigne> findByExerciceIdAndComptabiliseeFalse(UUID exerciceId);
}
