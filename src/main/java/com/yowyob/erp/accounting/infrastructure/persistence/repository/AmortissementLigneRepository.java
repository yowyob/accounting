package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.AmortissementLigneRepositoryPort;

import com.yowyob.erp.accounting.domain.model.AmortissementLigne;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface AmortissementLigneRepository extends R2dbcRepository<AmortissementLigne, UUID>, AmortissementLigneRepositoryPort {
    Flux<AmortissementLigne> findByImmoId(UUID immoId);

    Flux<AmortissementLigne> findByExerciceId(UUID exerciceId);

    Flux<AmortissementLigne> findByExerciceIdAndComptabiliseeFalse(UUID exerciceId);
}
